package app.ykspusula.monetize

import android.content.Context
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

/** Satın alma kaydının JSON içinden okunan alanları. */
data class PurchaseFields(val products: List<String>, val state: Int?, val packageName: String?)

/** Doğrulama sonucu. Kullanıcıya gösterilecek açıklamalar için ayrıntılı tutulur. */
enum class PurchaseCheck { OK, NO_KEY, BAD_SIGNATURE, WRONG_PRODUCT, NOT_PURCHASED, WRONG_PACKAGE, MALFORMED }

/**
 * Android'e bağımlı olmayan karar mantığı; birim testleriyle doğrulanır.
 *
 * Güvenlik sınırı: sunucu kullanılmadığı için doğrulama cihazda yapılır. Google Play'in imzası ve ürün
 * kimliği denetlenir; ancak root'lu veya değiştirilmiş bir cihazda uygulama kodu atlatılabilir.
 * Sunucu tarafı doğrulama olmadan tam sahtekârlık koruması yoktur.
 */
object EntitlementLogic {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    fun hash(token: String): String = MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }

    /** Hak yalnızca doğrulanmış PURCHASED kaydıyla verilir. Yalnızca Play açıkça "satın alma yok" derse geri alınır. */
    fun next(cached: CachedPurchase?, query: PurchaseQuery, now: Long): CachedPurchase? = when (query) {
        is PurchaseQuery.Purchased -> CachedPurchase(hash(query.token), query.json, query.signature, now)
        PurchaseQuery.None -> null
        PurchaseQuery.Pending -> cached
        PurchaseQuery.Unavailable -> cached
    }

    fun fields(text: String): PurchaseFields? = try {
        val o = json.parseToJsonElement(text) as kotlinx.serialization.json.JsonObject
        fun str(k: String) = (o[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content
        val list = (o["productIds"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        val products = list ?: listOfNotNull(str("productId"))
        val state = (o["purchaseState"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
        PurchaseFields(products, state, str("packageName"))
    } catch (e: Exception) { null }

    /**
     * Açık anahtar boşsa satın alma hiçbir zaman geçerli sayılmaz. İmza, ürün kimliği, paket adı ve
     * satın alma durumu (0 = tamamlandı) birlikte denetlenir.
     */
    fun check(jsonText: String, signature: String, publicKey: String, packageName: String = APP_PACKAGE): PurchaseCheck {
        if (publicKey.isBlank()) return PurchaseCheck.NO_KEY
        if (signature.isBlank() || !verifySignature(jsonText, signature, publicKey)) return PurchaseCheck.BAD_SIGNATURE
        val f = fields(jsonText) ?: return PurchaseCheck.MALFORMED
        if (REMOVE_ADS_PRODUCT !in f.products) return PurchaseCheck.WRONG_PRODUCT
        if (f.packageName != null && f.packageName != packageName) return PurchaseCheck.WRONG_PACKAGE
        if (f.state != null && f.state != 0) return PurchaseCheck.NOT_PURCHASED
        return PurchaseCheck.OK
    }

    fun isValid(c: CachedPurchase?, publicKey: String): Boolean =
        c != null && c.tokenHash.isNotBlank() && check(c.json, c.signature, publicKey) == PurchaseCheck.OK

    fun verifySignature(json: String, signature: String, publicKey: String): Boolean = try {
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(java.util.Base64.getDecoder().decode(publicKey)))
        Signature.getInstance("SHA1withRSA").run {
            initVerify(key)
            update(json.toByteArray())
            verify(java.util.Base64.getDecoder().decode(signature))
        }
    } catch (e: Exception) { false }

    fun message(c: PurchaseCheck): String = when (c) {
        PurchaseCheck.OK -> ""
        PurchaseCheck.NO_KEY -> "Bu sürümde satın alma doğrulaması yapılandırılmamış. Uygulamayı Google Play'den kurarak satın alabilirsin."
        PurchaseCheck.BAD_SIGNATURE, PurchaseCheck.MALFORMED -> "Satın alma kaydı doğrulanamadı. Reklamlar kaldırılmadı."
        PurchaseCheck.WRONG_PRODUCT, PurchaseCheck.WRONG_PACKAGE -> "Bu satın alma bu uygulamanın reklamsız ürününe ait değil."
        PurchaseCheck.NOT_PURCHASED -> "Ödeme henüz tamamlanmamış."
    }
}

const val APP_PACKAGE = "app.ykspusula"

/** Reklamsız hakkın tek merkezi. Bütün ekranlar ve reklam kodu bu akışı dinler. */
class EntitlementStore(private val context: Context, private val scope: CoroutineScope, val publicKey: String) {
    /** Bu derlemede satın alma doğrulanabiliyor mu? Yan yükleme sürümünde anahtar yoktur. */
    val purchasesEnabled: Boolean get() = publicKey.isNotBlank()
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
