package app.ykspusula.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.ykspusula.YksApp
import app.ykspusula.data.Repository
import app.ykspusula.data.UserProfile
import app.ykspusula.ui.components.Muted
import app.ykspusula.ui.screens.*
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.flow.map

/** Yalnızca otomatik testlerin ekranlara doğrudan gidebilmesi için. */
object DebugNav { @Volatile var nav: NavController? = null }

val LocalNav = staticCompositionLocalOf<NavController> { error("Nav yok") }

@Composable fun app(): YksApp = LocalContext.current.applicationContext as YksApp
@Composable fun repo(): Repository = app().repo

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "Ana Sayfa", Icons.Rounded.Home),
    Tab("study", "Çalış", Icons.Rounded.MenuBook),
    Tab("exams", "Denemeler", Icons.Rounded.Assignment),
    Tab("stats", "İstatistik", Icons.Rounded.BarChart),
    Tab("profile", "Profil", Icons.Rounded.Person),
)

@Composable
fun AppRoot() {
    val a = app()
    val ready by a.ready.collectAsStateWithLifecycle()
    val error by a.startError.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(Yks.c.bg)) {
        when {
            error != null -> Column(Modifier.align(Alignment.Center).padding(24.dp)) {
                Text("Uygulama başlatılamadı", style = MaterialTheme.typography.titleMedium, color = Yks.c.error)
                Muted(error ?: "")
            }
            !ready -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Yks.c.accent)
            else -> Gate()
        }
    }
}

@Composable
private fun Gate() {
    val r = repo()
    val loaded = remember { r.user.profile().map { Holder(it) } }
    val holder by loaded.collectAsStateWithLifecycle(null)
    val h = holder ?: return
    if (h.p?.onboarded == true) MainNav() else OnboardingScreen()
}

private class Holder(val p: UserProfile?)

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = TABS.any { it.route == route }
    DisposableEffect(nav) { DebugNav.nav = nav; onDispose { DebugNav.nav = null } }
    CompositionLocalProvider(LocalNav provides nav) {
        Scaffold(
            containerColor = Yks.c.bg,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (showBar) {
                    Column {
                        HorizontalDivider(thickness = 0.5.dp, color = Yks.c.line)
                        NavigationBar(containerColor = Yks.c.card, tonalElevation = 0.dp) {
                            TABS.forEach { t ->
                                NavigationBarItem(
                                    selected = route == t.route,
                                    onClick = {
                                        nav.navigate(t.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(t.icon, null) },
                                    label = { Text(t.label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Yks.c.accent, selectedTextColor = Yks.c.accent, indicatorColor = Yks.c.soft,
                                        unselectedIconColor = Yks.c.muted, unselectedTextColor = Yks.c.muted,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { inner ->
            Graph(nav, Modifier.padding(inner))
        }
    }
}

private fun enter(): EnterTransition = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 12 }
private fun exit(): ExitTransition = fadeOut(tween(140))

@Composable
private fun Graph(nav: NavHostController, modifier: Modifier) {
    NavHost(
        nav, "home", modifier,
        enterTransition = { enter() }, exitTransition = { exit() },
        popEnterTransition = { fadeIn(tween(160)) }, popExitTransition = { fadeOut(tween(120)) + slideOutHorizontally(tween(180)) { it / 12 } },
    ) {
        composable("home") { HomeScreen() }
        composable("study") { StudyScreen() }
        composable("exams") { ExamsScreen() }
        composable("stats") { StatsScreen() }
        composable("profile") { ProfileScreen() }
        composable("search") { SearchScreen() }

        composable("subject/{id}") { SubjectScreen(it.s("id")) }
        composable("topic/{id}") { TopicScreen(it.s("id")) }
        composable("tracker") { TrackerScreen() }
        composable("bank") { BankScreen() }
        composable(
            "solve?mode={mode}&exam={exam}&subject={subject}&topic={topic}&diff={diff}&state={state}&seed={seed}&limit={limit}&mix={mix}",
            listOf(
                sArg("mode", "BANK"), sArg("exam"), sArg("subject"), sArg("topic"),
                navArgument("diff") { type = NavType.IntType; defaultValue = 0 }, sArg("state", "ALL"),
                navArgument("seed") { type = NavType.LongType; defaultValue = 0L },
                navArgument("limit") { type = NavType.IntType; defaultValue = 0 },
                navArgument("mix") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { e ->
            SolveScreen(
                e.s("mode"), e.s("exam"), e.s("subject"), e.s("topic"), e.arguments?.getInt("diff") ?: 0, e.s("state"),
                e.arguments?.getLong("seed") ?: 0L, e.arguments?.getInt("limit") ?: 0, e.arguments?.getBoolean("mix") ?: false,
            )
        }
        composable("wrongs") { WrongsScreen() }
        composable("wrong/{id}") { WrongDetailScreen(it.s("id")) }
        composable("cards") { CardsScreen() }
        composable("deck/{deck}") { DeckScreen(it.s("deck")) }
        composable("review/{deck}") { CardReviewScreen(it.s("deck")) }
        composable("cardEdit/{deck}") { CardEditScreen(it.s("deck")) }
        composable("formulas") { FormulasScreen() }
        composable("notes") { NotesScreen() }
        composable("note/{id}") { NoteEditScreen(it.s("id")) }
        composable("library") { LibraryScreen() }
        composable("pdf/{id}") { PdfScreen(it.s("id")) }
        composable("pomodoro") { PomodoroScreen() }
        composable("plan") { PlanScreen() }
        composable("taskEdit/{date}") { TaskEditScreen(it.s("date")) }

        composable("examNew/{kind}") { ExamNewScreen(it.s("kind")) }
        composable("exam/{id}") { ExamRunScreen(it.s("id")) }
        composable("result/{id}") { ResultScreen(it.s("id")) }
        composable("examReview/{id}") { ExamReviewScreen(it.s("id")) }
        composable("external") { ExternalExamScreen() }
        composable("examSettings") { ExamSettingsScreen() }
        composable("obp") { ObpScreen() }

        composable("profileEdit") { ProfileEditScreen() }
        composable("settings") { SettingsScreen() }
        composable("reminders") { RemindersScreen() }
        composable("backup") { BackupScreen() }
        composable("targets") { TargetsScreen() }
        composable("packs") { PacksScreen() }
        composable("badges") { BadgesScreen() }
    }
}

private fun sArg(name: String, def: String = "") = navArgument(name) { type = NavType.StringType; defaultValue = def }
private fun NavBackStackEntry.s(k: String): String = arguments?.getString(k).orEmpty()

fun NavController.solve(
    mode: String = "BANK", exam: String = "", subject: String = "", topic: String = "", diff: Int = 0, state: String = "ALL",
    seed: Long = 0L, limit: Int = 0, mix: Boolean = false,
) = navigate("solve?mode=$mode&exam=$exam&subject=$subject&topic=$topic&diff=$diff&state=$state&seed=$seed&limit=$limit&mix=$mix")

/** Yerel tarih: 5 Kasım 2026 */
fun longDate(d: java.time.LocalDate): String = d.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("tr", "TR")))
fun shortDate(ms: Long): String = app.ykspusula.data.dateOf(ms).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("tr", "TR")))
