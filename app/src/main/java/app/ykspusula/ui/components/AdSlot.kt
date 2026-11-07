package app.ykspusula.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ykspusula.R
import app.ykspusula.monetize.AdPlacement
import app.ykspusula.monetize.AdTouchGuard
import app.ykspusula.ui.app
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Reklam alanı. Reklamsız hak varsa veya reklam yüklenemezse hiçbir şey çizilmez, boş kart kalmaz.
 * Yalnızca Ana Sayfa, Çalış, Denemeler listesi ve İstatistik akışında kullanılır.
 */
@Composable
fun AdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    val m = app().monetize
    val adsFree by m.entitlement.hasAdsFreeEntitlement.collectAsStateWithLifecycle()
    val can by m.ads.canRequestAds.collectAsStateWithLifecycle()
    val eligible by m.adsEligible.collectAsStateWithLifecycle()
    if (adsFree || !can || !eligible) return
    var ad by remember(placement) { mutableStateOf<NativeAd?>(null) }
    LaunchedEffect(placement) { m.ads.nativeFor(placement) { ad = it } }
    val current = ad ?: return
    NativeAdContainer(current, modifier)
}

@Composable
fun NativeAdContainer(ad: NativeAd, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { c -> AdTouchGuard(c).apply { addView(LayoutInflater.from(c).inflate(R.layout.ad_native, this, false)) } },
        modifier = modifier.fillMaxWidth(),
        update = { guard ->
            val v = guard.getChildAt(0) as NativeAdView
            val headline = v.findViewById<TextView>(R.id.ad_headline)
            val body = v.findViewById<TextView>(R.id.ad_body)
            val cta = v.findViewById<Button>(R.id.ad_cta)
            val advertiser = v.findViewById<TextView>(R.id.ad_advertiser)
            val media = v.findViewById<MediaView>(R.id.ad_media)
            v.headlineView = headline
            v.bodyView = body
            v.callToActionView = cta
            v.advertiserView = advertiser
            v.mediaView = media
            headline.text = ad.headline
            body.text = ad.body.orEmpty()
            cta.text = ad.callToAction ?: "Aç"
            advertiser.text = ad.advertiser.orEmpty()
            ad.mediaContent?.let { media.mediaContent = it }
            v.setNativeAd(ad)
        },
    )
}
