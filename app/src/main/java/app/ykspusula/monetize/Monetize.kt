package app.ykspusula.monetize

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.ykspusula.BuildConfig
import app.ykspusula.data.settingsStore
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
    val ads = AdsManager(appContext, scope, entitlement.hasAdsFreeEntitlement)
    private val _eligible = MutableStateFlow(false)
    /** İlk kullanımlarda reklam gösterilmez. */
    val adsEligible: StateFlow<Boolean> = _eligible
    private val launches = intPreferencesKey("launch_count")
    private var counted = false

    init {
        scope.launch {
            entitlement.hasAdsFreeEntitlement.drop(1).collect { free -> if (free) ads.clear() else ads.recheck() }
        }
        // 2.1.1'deki yaş ekranının cihazda bıraktığı doğum ayı ve yılı silinir; uygulama artık yaş saklamaz.
        scope.launch {
            appContext.settingsStore.edit { it.remove(intPreferencesKey("birth_year")); it.remove(intPreferencesKey("birth_month")) }
        }
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
