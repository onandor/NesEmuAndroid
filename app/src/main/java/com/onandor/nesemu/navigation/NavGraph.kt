package com.onandor.nesemu.navigation

import androidx.compose.foundation.layout.fillMaxSize
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
import com.onandor.nesemu.ui.screens.LibraryScreen
import com.onandor.nesemu.ui.screens.PreferencesScreen
import com.onandor.nesemu.viewmodels.NavigationViewModel

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavDestinations.LIBRARY_SCREEN,
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination
    val navManagerState by viewModel.navigationManager.navActions.collectAsState(null)

    LaunchedEffect(navManagerState) {
        navManagerState?.let {
            if (it.destination == NavDestinations.BACK) {
                navController.popBackStack()
            } else {
                navController.navigate(it.destination, it.navOptions)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(NavDestinations.LIBRARY_SCREEN) {
                LibraryScreen()
            }
            composable(NavDestinations.GAME_SCREEN) {
                GameScreen()
            }
            composable(NavDestinations.DEBUG_SCREEN) {
                DebugScreen()
            }
            composable(NavDestinations.PREFERENCES_SCREEN) {
                PreferencesScreen()
            }
        }
    }
}
