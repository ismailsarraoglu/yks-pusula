package app.ykspusula.monetize

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.ykspusula.data.settingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

const val REMOVE_ADS_PRODUCT = "remove_ads_lifetime"

/** Google Play sorgusunun sonucu. */
sealed interface PurchaseQuery {
    data class Purchased(val token: String, val json: String, val signature: String) : PurchaseQuery
    data object Pending : PurchaseQuery
    data object None : PurchaseQuery
    /** Play'e ulaşılamadı: önceden doğrulanmış hak korunur. */
    data object Unavailable : PurchaseQuery
}

data class CachedPurchase(val tokenHash: String, val json: String, val signature: String, val verifiedAt: Long)

/** Android'e bağımlı olmayan karar mantığı; birim testleriyle doğrulanır. */
object EntitlementLogic {
    fun hash(token: String): String = MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }

    /** Hak yalnızca doğrulanmış PURCHASED kaydıyla verilir. Yalnızca Play açıkça "satın alma yok" derse geri alınır. */
    fun next(cached: CachedPurchase?, query: PurchaseQuery, now: Long): CachedPurchase? = when (query) {
        is PurchaseQuery.Purchased -> CachedPurchase(hash(query.token), query.json, query.signature, now)
        PurchaseQuery.None -> null
        PurchaseQuery.Pending -> cached
        PurchaseQuery.Unavailable -> cached
    }

    /** Önbellek boş bir true/false değildir: kayıt ürünü içermeli ve anahtar tanımlıysa imzası tutmalıdır. */
    fun isValid(c: CachedPurchase?, publicKey: String): Boolean {
        if (c == null || c.tokenHash.isBlank() || !c.json.contains(REMOVE_ADS_PRODUCT)) return false
        return publicKey.isBlank() || verifySignature(c.json, c.signature, publicKey)
    }

    fun verifySignature(json: String, signature: String, publicKey: String): Boolean = try {
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(java.util.Base64.getDecoder().decode(publicKey)))
        Signature.getInstance("SHA1withRSA").run {
            initVerify(key)
            update(json.toByteArray())
            verify(java.util.Base64.getDecoder().decode(signature))
        }
    } catch (e: Exception) { false }
}

/** Reklamsız hakkın tek merkezi. Bütün ekranlar ve reklam kodu bu akışı dinler. */
class EntitlementStore(private val context: Context, private val scope: CoroutineScope, private val publicKey: String) {
    private val hashKey = stringPreferencesKey("ent_hash")
    private val jsonKey = stringPreferencesKey("ent_json")
    private val sigKey = stringPreferencesKey("ent_sig")
    private val timeKey = longPreferencesKey("ent_verified_at")
    private val _adsFree = MutableStateFlow(false)
    val hasAdsFreeEntitlement: StateFlow<Boolean> = _adsFree

    init { scope.launch { load() } }

    private suspend fun load() {
        val p = context.settingsStore.data.first()
        val c = p[hashKey]?.let { CachedPurchase(it, p[jsonKey].orEmpty(), p[sigKey].orEmpty(), p[timeKey] ?: 0) }
        _adsFree.value = EntitlementLogic.isValid(c, publicKey)
    }

    suspend fun apply(query: PurchaseQuery, now: Long = System.currentTimeMillis()) {
        val p = context.settingsStore.data.first()
        val cur = p[hashKey]?.let { CachedPurchase(it, p[jsonKey].orEmpty(), p[sigKey].orEmpty(), p[timeKey] ?: 0) }
        val next = EntitlementLogic.next(cur, query, now)
        context.settingsStore.edit { e ->
            if (next == null) { e.remove(hashKey); e.remove(jsonKey); e.remove(sigKey); e.remove(timeKey) }
            else { e[hashKey] = next.tokenHash; e[jsonKey] = next.json; e[sigKey] = next.signature; e[timeKey] = next.verifiedAt }
        }
        _adsFree.value = EntitlementLogic.isValid(next, publicKey)
    }
}
