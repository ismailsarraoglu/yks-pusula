package app.ykspusula.monetize

import android.app.Activity
import android.content.Context
import android.util.Log
import app.ykspusula.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
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
 *
 * Hedef kitle 18 yaş altını da içerir ve uygulama kullanıcının yaşını sormaz; bu yüzden her kullanıcı
 * "yaşı bilinmeyen" sayılır ve her reklam isteği çocuk korumasıyla yapılır:
 * - Google Play Families Self-Certified Ads SDK listesindeki Google Mobile Ads SDK (`play-services-ads`),
 * - çocuğa yönelik işlem (TFCD) ve rıza yaşı altı (TFUA) etiketleri: kişiselleştirilmiş reklam ve yeniden pazarlama kapalı,
 *   reklam kimliği gönderilmez,
 * - en yüksek içerik derecesi G (genel izleyici),
 * - reklam kimliği izni (AD_ID) manifestten kaldırılmıştır.
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
    /** Rıza ve başlatma zincirinde gelinen son adım. Yalnızca teşhis için tutulur. */
    @Volatile private var stage = "baslamadi"

    init {
        // Yapılandırma SDK başlatılmadan önce verilir; ilk istekten itibaren geçerlidir.
        runCatching { MobileAds.setRequestConfiguration(requestConfiguration()) }.onFailure { note("Reklam yapilandirmasi: $it") }
    }

    fun onActivityStart(activity: Activity) {
        if (entitlement.value) { _can.value = false; return }
        // Rıza formu da yaşı bilinmeyen kullanıcı için rıza yaşı altı kabulüyle istenir.
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(true).build()
        try {
            step("riza istendi")
            consent.requestConsentInfoUpdate(activity, params, {
                step("riza bilgisi geldi")
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { e ->
                    if (e != null) note("Riza formu: ${e.errorCode} ${e.message}")
                    step("riza tamam")
                    update()
                }
            }, { e -> note("Riza bilgisi: ${e.errorCode} ${e.message}"); update() })
        } catch (e: Exception) { note("Riza: $e"); update() }
        update()
    }

    private fun step(name: String) {
        stage = name
        Log.i("YksAds", name)
    }

    /** Test raporları için anlık durum özeti. Kişisel veri içermez. */
    fun debugState() = "asama=$stage canRequestAds=${consent.canRequestAds()} riza=${consent.consentStatus} basladi=$started hazir=$initialized"

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
                step("reklam baslatma istendi")
                MobileAds.setRequestConfiguration(requestConfiguration())
                MobileAds.initialize(context.applicationContext) {
                    step("reklam baslatma bitti")
                    initialized = true
                    scope.launch(Dispatchers.Main) { update() }
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
        try {
            val loader = AdLoader.Builder(context.applicationContext, AdsConfig.nativeUnit)
                .forNativeAd { nativeAd ->
                    if (entitlement.value) { nativeAd.destroy(); onReady(null); return@forNativeAd }
                    cache.remove(placement)?.first?.destroy()
                    cache[placement] = nativeAd to System.currentTimeMillis()
                    onReady(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        note("Reklam yuklenemedi: $adError")
                        failedAt[placement] = System.currentTimeMillis()
                        onReady(null)
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
            loader.loadAd(AdRequest.Builder().build())
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

    /** Hak değişince çağrılır. */
    fun recheck() { update() }

    companion object {
        /** Bütün reklam istekleri için çocuk korumalı yapılandırma (bkz. sınıf açıklaması). */
        @Suppress("DEPRECATION")
        fun requestConfiguration(): RequestConfiguration = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()
    }
}
