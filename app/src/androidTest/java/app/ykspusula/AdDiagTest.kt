package app.ykspusula

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ykspusula.data.UserProfile
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Geçici teşhis: reklam türlerini ayrı ayrı dener, sonucu günlüğe yazar. Hiçbir denetim yapmaz. */
@RunWith(AndroidJUnit4::class)
class AdDiagTest {
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as YksApp
    private fun say(s: String) { s.chunked(3500).forEach { Log.w("YksTani", it) } }

    private fun native(name: String, unit: String, type: NativeAd.NativeAdType) {
        val latch = CountDownLatch(1)
        val req = NativeAdRequest.Builder(unit, listOf(type)).build()
        NativeAdLoader.load(req, object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) { say("$name YUKLENDI baslik=${nativeAd.headline}"); latch.countDown() }
            override fun onAdFailedToLoad(adError: LoadAdError) { say("$name HATA kod=${adError.code} mesaj=${adError.message} neden=${adError.cause}"); latch.countDown() }
        })
        if (!latch.await(60, TimeUnit.SECONDS)) say("$name ZAMAN ASIMI")
    }

    @Test fun variants() {
        runBlocking {
            app.ready.first { it }
            app.repo.user.saveProfile(UserProfile(onboarded = true, name = "Test"))
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        app.monetize.forceEligibleForTest()
        val end = System.currentTimeMillis() + 60_000
        while (!app.monetize.ads.canRequestAds.value && System.currentTimeMillis() < end) Thread.sleep(200)
        say("izin=${app.monetize.ads.canRequestAds.value}")
        native("yerel", "ca-app-pub-3940256099942544/2247696110", NativeAd.NativeAdType.NATIVE)
        native("yerel-video", "ca-app-pub-3940256099942544/1044960115", NativeAd.NativeAdType.NATIVE)
        val latch = CountDownLatch(1)
        var act: Activity? = null
        scenario.onActivity { a ->
            act = a
            val view = AdView(a)
            a.setContentView(view)
            val req = BannerAdRequest.Builder("ca-app-pub-3940256099942544/9214589741", AdSize.getLargeAnchoredAdaptiveBannerAdSize(a, 360)).build()
            view.loadAd(req, object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) { say("banner YUKLENDI"); latch.countDown() }
                override fun onAdFailedToLoad(adError: LoadAdError) { say("banner HATA kod=${adError.code} mesaj=${adError.message}"); latch.countDown() }
            })
        }
        if (!latch.await(60, TimeUnit.SECONDS)) say("banner ZAMAN ASIMI")
        scenario.close()
    }
}
