package app.ykspusula.monetize

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class BillingUi(
    val connected: Boolean = false,
    val price: String? = null,
    val pending: Boolean = false,
    val message: String = "",
    val busy: Boolean = false,
)

/** Play Billing sarmalayıcı. Sunucu kullanılmaz: doğrulama istemci tarafındadır. */
class BillingManager(context: Context, private val scope: CoroutineScope, private val store: EntitlementStore) : PurchasesUpdatedListener {
    private val _ui = MutableStateFlow(BillingUi())
    val ui: StateFlow<BillingUi> = _ui
    private var details: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (client.isReady) { scope.launch { refresh() }; return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                    _ui.value = _ui.value.copy(connected = true, message = "")
                    scope.launch { refresh() }
                } else {
                    _ui.value = _ui.value.copy(connected = false, message = "Google Play satın alma servisine ulaşılamadı.")
                    scope.launch { store.apply(PurchaseQuery.Unavailable) }
                }
            }
            override fun onBillingServiceDisconnected() { _ui.value = _ui.value.copy(connected = false) }
        })
    }

    private suspend fun refresh() {
        loadProduct()
        queryPurchases()
    }

    private suspend fun loadProduct() = suspendCancellableCoroutine { c ->
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(REMOVE_ADS_PRODUCT).setProductType(BillingClient.ProductType.INAPP).build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { r, result ->
            val d = result.productDetailsList.firstOrNull()
            details = d
            val offer = d?.oneTimePurchaseOfferDetailsList?.firstOrNull()
            _ui.value = _ui.value.copy(price = offer?.formattedPrice, message = if (r.responseCode == BillingClient.BillingResponseCode.OK && d != null) "" else "Ürün bilgisi alınamadı.")
            if (c.isActive) c.resume(Unit)
        }
    }

    /** Uygulama açıldığında, ön plana döndüğünde ve geri yüklemede çalışır. */
    suspend fun queryPurchases() {
        if (!client.isReady) { store.apply(PurchaseQuery.Unavailable); return }
        val (ok, list) = suspendCancellableCoroutine<Pair<Boolean, List<Purchase>>> { c ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { r, purchases ->
                if (c.isActive) c.resume((r.responseCode == BillingClient.BillingResponseCode.OK) to purchases)
            }
        }
        if (!ok) { store.apply(PurchaseQuery.Unavailable); return }
        handle(list.filter { REMOVE_ADS_PRODUCT in it.products }, restoring = true)
    }

    fun restore() {
        _ui.value = _ui.value.copy(busy = true, message = "")
        scope.launch {
            queryPurchases()
            _ui.value = _ui.value.copy(busy = false, message = if (store.hasAdsFreeEntitlement.value) "Satın alma geri yüklendi." else if (_ui.value.connected) "Bu Google Play hesabında satın alma bulunamadı." else "Google Play'e ulaşılamadı. İnternet bağlantını kontrol et.")
        }
    }

    fun buy(activity: Activity) {
        if (!store.purchasesEnabled) { _ui.value = _ui.value.copy(message = EntitlementLogic.message(PurchaseCheck.NO_KEY)); return }
        val d = details
        val offer = d?.oneTimePurchaseOfferDetailsList?.firstOrNull()
        if (d == null || offer == null) { _ui.value = _ui.value.copy(message = "Ürün bilgisi henüz alınamadı. Bağlantını kontrol edip yeniden dene."); connect(); return }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).setOfferToken(offer.offerToken.orEmpty()).build())
        ).build()
        val r = client.launchBillingFlow(activity, params)
        if (r.responseCode != BillingClient.BillingResponseCode.OK) _ui.value = _ui.value.copy(message = text(r.responseCode))
    }

    override fun onPurchasesUpdated(r: BillingResult, purchases: List<Purchase>?) {
        when (r.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { handle(purchases.orEmpty().filter { REMOVE_ADS_PRODUCT in it.products }, restoring = false) }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { queryPurchases() }
            else -> _ui.value = _ui.value.copy(message = text(r.responseCode))
        }
    }

    private fun text(code: Int) = when (code) {
        BillingClient.BillingResponseCode.USER_CANCELED -> "Satın alma iptal edildi."
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, BillingClient.BillingResponseCode.NETWORK_ERROR -> "Bağlantı kurulamadı. Daha sonra yeniden dene."
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Bu cihazda Google Play satın alma kullanılamıyor."
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "Reklamlar zaten kaldırılmış."
        else -> "Satın alma tamamlanamadı. Herhangi bir ücret alınmadıysa yeniden deneyebilirsin."
    }

    private suspend fun handle(list: List<Purchase>, restoring: Boolean) {
        val done = list.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val waiting = list.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        _ui.value = _ui.value.copy(pending = waiting && done == null)
        when {
            done != null -> {
                // Önce Google Play imzası ve ürün denetlenir; geçmeyen kayıt onaylanmaz ve hak açmaz.
                val check = EntitlementLogic.check(done.originalJson, done.signature, store.publicKey)
                if (check != PurchaseCheck.OK) {
                    store.apply(PurchaseQuery.Unavailable)
                    _ui.value = _ui.value.copy(message = EntitlementLogic.message(check))
                    return
                }
                val ok = done.isAcknowledged || acknowledge(done)
                if (ok) {
                    store.apply(PurchaseQuery.Purchased(done.purchaseToken, done.originalJson, done.signature))
                    if (!restoring) _ui.value = _ui.value.copy(message = "Teşekkürler. Reklamlar kaldırıldı.")
                } else {
                    store.apply(PurchaseQuery.Unavailable)
                    _ui.value = _ui.value.copy(message = "Satın alma onaylanamadı. Uygulama yeniden açıldığında tekrar denenecek.")
                }
            }
            waiting -> {
                store.apply(PurchaseQuery.Pending)
                _ui.value = _ui.value.copy(message = "Ödemen bekleniyor. Tamamlandığında reklamlar kalkacak.")
            }
            else -> store.apply(PurchaseQuery.None)
        }
    }

    private suspend fun acknowledge(p: Purchase): Boolean = suspendCancellableCoroutine { c ->
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r ->
            if (c.isActive) c.resume(r.responseCode == BillingClient.BillingResponseCode.OK)
        }
    }
}
