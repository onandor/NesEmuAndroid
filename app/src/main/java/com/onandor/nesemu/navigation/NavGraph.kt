package com.onandor.nesemu.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onandor.nesemu.ui.screens.DebugScreen
import com.onandor.nesemu.ui.screens.GameScreen
import com.onandor.nesemu.ui.screens.MainScreen
import com.onandor.nesemu.ui.screens.PreferencesScreen
import com.onandor.nesemu.viewmodels.NavigationViewModel

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavDestinations.MAIN_SCREEN,
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination
    val navManagerState by viewModel.navigationManager.navActions.collectAsState(null)

    LaunchedEffect(navManagerState) {
        navManagerState?.let {
            try {
                if (it.destination == NavDestinations.BACK) {
                    navController.popBackStack()
                } else {
                    navController.navigate(it.destination, it.navOptions)
                }
            } catch (_: IllegalArgumentException) {
                /* Sometimes Live Edit has issues here with the graph, this solves it */
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(PaddingValues()),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(
                    route = NavDestinations.MAIN_SCREEN,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    MainScreen()
                }
                composable(
                    route = NavDestinations.GAME_SCREEN,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    GameScreen()
                }
                composable(
                    route = NavDestinations.DEBUG_SCREEN,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    DebugScreen()
                }
                composable(
                    route = NavDestinations.PREFERENCES_SCREEN,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    PreferencesScreen()
                }
            }
        }
    }
}
