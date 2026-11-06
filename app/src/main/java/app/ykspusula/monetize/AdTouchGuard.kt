package app.ykspusula.monetize

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Reklamın üstünden başlayan sürükleme tıklamaya dönüşmesin diye reklamı sarar.
 *
 * Parmak kalkarken dokunma noktası başlangıçtan uzaklaşmışsa hareket dokunma değil kaydırmadır.
 * Bu durumda alt görünümlere parmak kalktı yerine iptal gönderilir. Gerçek dokunuşlara karışmaz.
 * Yanlışlıkla tıklama AdMob politikalarına aykırıdır; bu yüzden yalnızca reklamda kullanılır.
 */
class AdTouchGuard(context: Context) : FrameLayout(context) {
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragged = false

    private fun movedFar(ev: MotionEvent) = abs(ev.rawX - downX) > slop || abs(ev.rawY - downY) > slop

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = ev.rawX; downY = ev.rawY; dragged = false }
            MotionEvent.ACTION_MOVE -> if (movedFar(ev)) dragged = true
            MotionEvent.ACTION_UP -> if (dragged || movedFar(ev)) {
                Log.i("YksAds", "surukleme reklami acmadan iptal edildi")
                val cancel = MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL }
                val handled = super.dispatchTouchEvent(cancel)
                cancel.recycle()
                return handled
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
