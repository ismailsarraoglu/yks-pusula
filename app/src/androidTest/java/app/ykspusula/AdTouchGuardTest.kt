package app.ykspusula

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ykspusula.monetize.AdTouchGuard
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Sürükleme reklama ulaşmamalı, gerçek dokunuş ulaşmalı. Reklam ve internet gerektirmez. */
@RunWith(AndroidJUnit4::class)
class AdTouchGuardTest {
    private val d = MotionEvent.ACTION_DOWN
    private val m = MotionEvent.ACTION_MOVE
    private val u = MotionEvent.ACTION_UP

    /** Olayları koruyucuya verir, alt görünüme ulaşan parmak kalkma sayısını döner. */
    private fun upsReachingAd(vararg events: Triple<Int, Float, Float>): Int {
        var ups = 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val c = ApplicationProvider.getApplicationContext<Context>()
            val guard = AdTouchGuard(c)
            val ad = object : View(c) {
                override fun onTouchEvent(e: MotionEvent): Boolean {
                    if (e.actionMasked == MotionEvent.ACTION_UP) ups++
                    return true
                }
            }
            guard.addView(ad, FrameLayout.LayoutParams(800, 800))
            val exact = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
            guard.measure(exact, exact)
            guard.layout(0, 0, 800, 800)
            val t = SystemClock.uptimeMillis()
            events.forEachIndexed { i, (action, x, y) ->
                val e = MotionEvent.obtain(t, t + i * 10L, action, x, y, 0)
                guard.dispatchTouchEvent(e)
                e.recycle()
            }
        }
        return ups
    }

    @Test fun tapStillReachesAd() {
        assertEquals(1, upsReachingAd(Triple(d, 100f, 100f), Triple(u, 100f, 100f)))
        assertEquals("Parmağın çok az oynaması dokunuşu bozmaz", 1, upsReachingAd(Triple(d, 100f, 100f), Triple(m, 102f, 101f), Triple(u, 102f, 101f)))
    }

    @Test fun dragWithoutMoveEventsIsNotAClick() {
        assertEquals(0, upsReachingAd(Triple(d, 100f, 600f), Triple(u, 100f, 100f)))
    }

    @Test fun dragThatComesBackIsNotAClick() {
        assertEquals(0, upsReachingAd(Triple(d, 100f, 600f), Triple(m, 100f, 300f), Triple(m, 100f, 600f), Triple(u, 100f, 600f)))
    }
}
