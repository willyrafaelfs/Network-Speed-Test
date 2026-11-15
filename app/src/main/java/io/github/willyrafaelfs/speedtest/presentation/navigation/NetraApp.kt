package io.github.willyrafaelfs.speedtest.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import io.github.willyrafaelfs.speedtest.R
import io.github.willyrafaelfs.speedtest.presentation.history.HistoryScreen
import io.github.willyrafaelfs.speedtest.presentation.speedtest.SpeedTestScreen
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

/** Route type-safe: argumen & tujuan navigasi dicek compiler, bukan string. */
@Serializable
data object TestRoute

@Serializable
data object HistoryRoute

/** Tujuan level teratas yang tampil di bottom bar / navigation rail. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    TEST(TestRoute, TestRoute::class, R.string.nav_test, R.drawable.ic_nav_test),
    HISTORY(HistoryRoute, HistoryRoute::class, R.string.nav_history, R.drawable.ic_nav_history),
}

/**
 * Root aplikasi. [NavigationSuiteScaffold] memilih bottom bar di layar sempit dan
 * navigation rail di layar lebar (tablet, landscape), sesuai WindowSizeClass.
 */
@Composable
fun NetraApp(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    // Ponsel landscape: tinggi sempit, jadi rail di samping lebih hemat ruang daripada bar bawah.
    val layoutType = if (adaptiveInfo.windowSizeClass.isHeightAtLeastBreakpoint(
            WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
        )
    ) {
        NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
    } else {
        NavigationSuiteType.NavigationRail
    }

    NavigationSuiteScaffold(
        modifier = modifier.fillMaxSize(),
        layoutType = layoutType,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = currentDestination?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true,
                    onClick = { navController.navigateToTopLevel(destination) },
                    // Label sudah dibacakan; ikon dekoratif agar tidak dibaca dua kali.
                    icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            // Inset bawah ditangani bar navigasi; inset samping tetap perlu untuk landscape
            // (tombol navigasi sistem & display cutout bisa berada di kiri/kanan).
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TestRoute,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable<TestRoute> { SpeedTestScreen(snackbarHostState = snackbarHostState) }
                composable<HistoryRoute> {
                    HistoryScreen(onStartTest = { navController.navigateToTopLevel(TopLevelDestination.TEST) })
                }
            }
        }
    }
}

/** Pindah antar tab tanpa menumpuk back stack, dan mengembalikan state tab sebelumnya. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
