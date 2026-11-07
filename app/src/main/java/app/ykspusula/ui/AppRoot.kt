package app.ykspusula.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
import app.ykspusula.ui.components.AppBackground
import app.ykspusula.ui.components.Muted
import app.ykspusula.ui.screens.*
import app.ykspusula.ui.theme.Yks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map

/** Yalnızca otomatik testlerin ekranlara doğrudan gidebilmesi için. */
object DebugNav { @Volatile var nav: NavController? = null }

val LocalNav = staticCompositionLocalOf<NavController> { error("Nav yok") }

@Composable fun app(): YksApp = LocalContext.current.applicationContext as YksApp
@Composable fun repo(): Repository = app().repo
/** Gün değişince ekranlar yeni günün verisini gösterir. */
@Composable fun currentDay(): java.time.LocalDate = app().day.collectAsStateWithLifecycle().value

/** Askıya alınan işlerden sonra gezinme her zaman ana iş parçacığında sürer. */
@Composable fun rememberUiScope(): CoroutineScope {
    val base = rememberCoroutineScope()
    return remember(base) { CoroutineScope(base.coroutineContext + Dispatchers.Main.immediate) }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "Ana Sayfa", Icons.Rounded.Home),
    Tab("study", "Çalış", Icons.AutoMirrored.Rounded.MenuBook),
    Tab("exams", "Denemeler", Icons.Rounded.Assignment),
    Tab("stats", "İstatistik", Icons.Rounded.BarChart),
    Tab("profile", "Profil", Icons.Rounded.Person),
)

@Composable
fun AppRoot() {
    val a = app()
    val ready by a.ready.collectAsStateWithLifecycle()
    val error by a.startError.collectAsStateWithLifecycle()
    AppBackground {
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

/** Alt menü: aktif sekme mavi ve altında ince çizgi. Tam ekran odak ve deneme oturumlarında gizlenir. */
@Composable
private fun BottomNavigationBar(route: String?, onSelect: (String) -> Unit) {
    val c = Yks.c
    // Yatayda kamera kesiği ve gezinme çubuğu yanda kalır; sekmeler bu alanların altına girmesin (Android 15 kenardan kenara).
    Column(Modifier.background(if (c.isDark) c.bg2 else c.card).windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)))) {
        HorizontalDivider(thickness = 0.5.dp, color = c.line)
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).selectableGroup()) {
            TABS.forEach { t ->
                val selected = route == t.route
                val tint by animateColorAsState(if (selected) c.accentLight else c.muted, tween(180), label = "sekme")
                val line by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "cizgi")
                Column(
                    Modifier.weight(1f).selectable(selected, role = Role.Tab) { onSelect(t.route) }.padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(t.icon, null, tint = tint, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(t.label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width(44.dp * line).height(3.dp).clip(CircleShape).background(c.accent))
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val a = app()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = TABS.any { it.route == route }
    val pending by a.pendingRoute.collectAsStateWithLifecycle()
    DisposableEffect(nav) { DebugNav.nav = nav; onDispose { DebugNav.nav = null } }
    LaunchedEffect(pending, route) {
        val target = pending
        if (target != null && route != null) {
            a.pendingRoute.value = null
            runCatching {
                if (TABS.any { it.route == target }) nav.navigate(target) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                else nav.navigate(target)
            }
        }
    }
    CompositionLocalProvider(LocalNav provides nav) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (showBar) BottomNavigationBar(route) { r ->
                    nav.navigate(r) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
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
        composable("mastery") { MasteryScreen() }
        composable("priorities") { PrioritiesScreen() }
        composable("log?source={source}&topic={topic}&subject={subject}&task={task}&route={route}", listOf(sArg("source"), sArg("topic"), sArg("subject"), sArg("task"), sArg("route"))) {
            StudyLogScreen(it.s("source"), it.s("topic"), it.s("subject"), it.s("task"), it.s("route"))
        }
        composable("sources") { SourcesScreen() }
        composable("source/{id}") { SourceDetailScreen(it.s("id")) }
        composable("sourceEdit/{id}") { SourceEditScreen(it.s("id")) }
        composable("sourceCompare") { SourceCompareScreen() }
        composable("wrongs") { WrongsScreen() }
        composable("wrong/{id}") { WrongDetailScreen(it.s("id")) }
        composable("wrongEdit/{id}?topic={topic}&subject={subject}&source={source}", listOf(sArg("topic"), sArg("subject"), sArg("source"))) { WrongEditScreen(it.s("id"), it.s("topic"), it.s("subject"), it.s("source")) }
        composable("wrongReview") { WrongReviewScreen() }
        composable("cards") { CardsScreen() }
        composable("deck/{deck}") { DeckScreen(it.s("deck")) }
        composable("review/{deck}?mix={mix}", listOf(sArg("mix"))) { CardReviewScreen(it.s("deck"), it.s("mix") == "1") }
        composable("cardEdit/{deck}") { CardEditScreen(it.s("deck")) }
        composable("formulas") { FormulasScreen() }
        composable("notes") { NotesScreen() }
        composable("note/{id}") { NoteEditScreen(it.s("id")) }
        composable("library") { LibraryScreen() }
        composable("pdf/{id}") { PdfScreen(it.s("id")) }
        composable(
            "pomodoro?subject={subject}&topic={topic}&source={source}&minutes={minutes}&label={label}&routeId={routeId}",
            listOf(sArg("subject"), sArg("topic"), sArg("source"), navArgument("minutes") { type = NavType.IntType; defaultValue = 0 }, sArg("label"), sArg("routeId")),
        ) { PomodoroScreen(it.s("subject"), it.s("topic"), it.s("source"), it.arguments?.getInt("minutes") ?: 0, it.s("label"), it.s("routeId")) }
        composable("plan") { PlanScreen() }
        composable("planSettings") { PlanSettingsScreen() }
        composable("planHistory") { PlanHistoryScreen() }
        composable("taskEdit/{date}") { TaskEditScreen(it.s("date")) }
        composable("coach") { CoachScreen() }
        composable("replan") { ReplanScreen() }

        composable("examStart/{kind}") { ExamStartScreen(it.s("kind")) }
        composable("examRun/{id}") { ExamRunScreen(it.s("id")) }
        composable("examEntry/{id}") { ExamEntryScreen(it.s("id")) }
        composable("result/{id}") { ResultScreen(it.s("id")) }
        composable("examHistory") { ExamHistoryScreen() }
        composable("examCompare/{a}/{b}") { ExamCompareScreen(it.s("a"), it.s("b")) }
        composable("examAnalysis") { ExamAnalysisScreen() }
        composable("netGoals") { NetGoalsScreen() }
        composable("external?kind={kind}&exam={exam}&subject={subject}&title={title}", listOf(sArg("kind"), sArg("exam"), sArg("subject"), sArg("title"))) {
            ExternalExamScreen(it.s("kind"), it.s("exam"), it.s("subject"), it.s("title"))
        }
        composable("examSettings") { ExamSettingsScreen() }
        composable("obp") { ObpScreen() }

        composable("reports") { ReportsScreen() }
        composable("motivation") { MotivationScreen() }
        composable("removeAds") { RemoveAdsScreen() }
        composable("privacy") { TextScreen("Gizlilik politikası", POLICY_TEXT) }
        composable("terms") { TextScreen("Satın alma koşulları", TERMS_TEXT) }
        composable("about") { AboutScreen() }
        composable("widgets") { WidgetsScreen() }
        composable("info") { InfoScreen() }
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

/** Yerel tarih: 5 Kasım 2026 */
fun longDate(d: java.time.LocalDate): String = d.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("tr", "TR")))
fun shortDate(ms: Long): String = app.ykspusula.data.dateOf(ms).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("tr", "TR")))
