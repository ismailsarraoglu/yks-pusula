package app.ykspusula.monetize

import android.app.Activity
import android.content.Context
import android.util.Log
import app.ykspusula.BuildConfig
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Reklam kimlikleri tek yerden yönetilir. Üretim değerleri derleme ayarından gelir. */
object AdsConfig {
    val appId: String get() = BuildConfig.ADMOB_APP_ID
    val bannerUnit: String get() = BuildConfig.ADMOB_BANNER_ID
    val nativeUnit: String get() = BuildConfig.ADMOB_NATIVE_ID
    /** Aynı reklam alanı en çok bu sıklıkta yeni reklam ister. */
    const val MIN_REFRESH_MS = 90_000L
    /** İlk kullanımlarda reklam gösterilmez. */
    const val MIN_LAUNCHES = 3
}

/** Öğrenci reklam görmemesi gereken ekranlarda reklam alanı hiç oluşturulmaz. */
enum class AdPlacement { HOME, STUDY, EXAMS, STATS }

/**
 * Rıza, reklam başlatma ve önbellek. Reklamsız hak açıldığında hiçbir istek yapılmaz.
 * Hata olursa uygulama çalışmaya devam eder; reklam alanı görünmez.
 */
class AdsManager(private val context: Context, private val scope: CoroutineScope, private val entitlement: StateFlow<Boolean>) {
    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(context.applicationContext)
    private val _can = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _can
    private val _privacyRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyRequired
    private var started = false
    @Volatile private var initialized = false
    private val cache = HashMap<AdPlacement, Pair<NativeAd, Long>>()
    private val failedAt = HashMap<AdPlacement, Long>()
    /** Son yükleme hatası. Yalnızca teşhis için tutulur, hiçbir yere gönderilmez. */
    @Volatile var lastError: String? = null
        private set

    /** Tüm uygulama için geçerli: içerik düzeyi gençlere uygun tutulur. */
    private fun requestConfiguration() = RequestConfiguration.Builder()
        .setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_PG)
        .build()

    fun onActivityStart(activity: Activity) {
        if (entitlement.value) { _can.value = false; return }
        val params = ConsentRequestParameters.Builder().build()
        try {
            consent.requestConsentInfoUpdate(activity, params, {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { e ->
                    if (e != null) note("Riza formu: ${e.errorCode} ${e.message}")
                    update()
                }
            }, { e -> note("Riza bilgisi: ${e.errorCode} ${e.message}"); update() })
        } catch (e: Exception) { note("Riza: $e"); update() }
        update()
    }

    private fun note(message: String) {
        lastError = message
        Log.w("YksAds", message)
    }

    private fun update() {
        _privacyRequired.value = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        val allowed = consent.canRequestAds() && !entitlement.value
        if (allowed) initialize()
        _can.value = allowed && initialized
    }

    private fun initialize() {
        if (started) return
        started = true
        scope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(context.applicationContext, InitializationConfig.Builder(AdsConfig.appId).setRequestConfiguration(requestConfiguration()).build()) {
                    initialized = true
                    update()
                }
            } catch (e: Throwable) {
                note("Reklam baslatilamadi: $e")
                started = false
                _can.value = false
            }
        }
    }

    fun showPrivacyOptions(activity: Activity, done: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> update(); done() }
    }

    fun nativeFor(placement: AdPlacement, onReady: (NativeAd?) -> Unit) {
        if (entitlement.value || !_can.value) { onReady(null); return }
        val now = System.currentTimeMillis()
        cache[placement]?.let { (ad, at) -> if (now - at < AdsConfig.MIN_REFRESH_MS * 3) { onReady(ad); return } }
        if (now - (failedAt[placement] ?: 0L) < AdsConfig.MIN_REFRESH_MS) { onReady(null); return }
        val request = NativeAdRequest.Builder(AdsConfig.nativeUnit, listOf(NativeAd.NativeAdType.NATIVE)).build()
        try {
            NativeAdLoader.load(request, object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    scope.launch(Dispatchers.Main) {
                        if (entitlement.value) { nativeAd.destroy(); onReady(null); return@launch }
                        cache.remove(placement)?.first?.destroy()
                        cache[placement] = nativeAd to System.currentTimeMillis()
                        onReady(nativeAd)
                    }
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    note("Reklam yuklenemedi: $adError")
                    failedAt[placement] = System.currentTimeMillis()
                    scope.launch(Dispatchers.Main) { onReady(null) }
                }
            })
        } catch (e: Throwable) {
            lastError = e.toString()
            failedAt[placement] = now
            onReady(null)
        }
    }

    /** Satın alma tamamlanınca bütün reklamlar bellekten silinir. */
    fun clear() {
        cache.values.forEach { it.first.destroy() }
        cache.clear()
        _can.value = false
    }

    fun recheck() { update() }
}
