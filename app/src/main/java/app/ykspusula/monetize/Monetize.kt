package app.ykspusula.monetize

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.ykspusula.BuildConfig
import app.ykspusula.data.settingsStore
import app.ykspusula.data.today
import app.ykspusula.domain.AgeGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Gelir modelinin tek giriş noktası. */
class Monetize(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext
    val entitlement = EntitlementStore(appContext, scope, BuildConfig.PLAY_PUBLIC_KEY)
    val billing = BillingManager(appContext, scope, entitlement)
    private val birthYearKey = intPreferencesKey("birth_year")
    private val birthMonthKey = intPreferencesKey("birth_month")
    private val _birth = MutableStateFlow<Pair<Int, Int>?>(null)
    /** (yıl, ay). null: henüz okunmadı. Yıl 0 girilmedi, -1 belirtilmedi. Yalnızca cihazda saklanır. */
    val birth: StateFlow<Pair<Int, Int>?> = _birth
    private val _adult = MutableStateFlow(false)
    /** Reklam yalnızca 18 yaşını doldurduğu tarafsız yaş ekranıyla belirlenen kullanıcılara istenir. */
    val verifiedAdult: StateFlow<Boolean> = _adult
    val ads = AdsManager(appContext, scope, entitlement.hasAdsFreeEntitlement, _adult)
    private val _eligible = MutableStateFlow(false)
    /** İlk kullanımlarda reklam gösterilmez. */
    val adsEligible: StateFlow<Boolean> = _eligible
    private val launches = intPreferencesKey("launch_count")
    private var counted = false

    init {
        scope.launch {
            entitlement.hasAdsFreeEntitlement.drop(1).collect { free -> if (free) ads.clear() else ads.recheck() }
        }
        scope.launch {
            appContext.settingsStore.data.collect { p ->
                val b = (p[birthYearKey] ?: AgeGate.NOT_SET) to (p[birthMonthKey] ?: 0)
                _birth.value = b
                val adult = AgeGate.isVerifiedAdult(b.first, b.second, today())
                if (adult != _adult.value) { _adult.value = adult; if (adult) ads.recheck() else ads.clear() }
            }
        }
    }

    /** Tarafsız yaş ekranının cevabı. Yıl -1 ise kullanıcı belirtmek istemedi. */
    suspend fun saveBirth(year: Int, month: Int) {
        appContext.settingsStore.edit { it[birthYearKey] = year; it[birthMonthKey] = month }
    }

    fun onActivityStart(activity: Activity) {
        if (!counted) {
            counted = true
            scope.launch {
                var n = 0
                appContext.settingsStore.edit { e -> n = (e[launches] ?: 0) + 1; e[launches] = n }
                _eligible.value = n >= AdsConfig.MIN_LAUNCHES
            }
        }
        billing.connect()
        ads.onActivityStart(activity)
    }

    /** Yalnızca otomatik testler için. */
    fun forceEligibleForTest() { _eligible.value = true }
}
