package com.onandor.nesemu.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
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
import com.onandor.nesemu.ui.screens.SettingsScreen
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

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(NavDestinations.MAIN_SCREEN) {
                MainScreen()
            }
            composable(NavDestinations.GAME_SCREEN) {
                GameScreen()
            }
            composable(NavDestinations.DEBUG_SCREEN) {
                DebugScreen()
            }
            composable(NavDestinations.SETTINGS_SCREEN) {
                SettingsScreen()
            }
        }
    }
}
