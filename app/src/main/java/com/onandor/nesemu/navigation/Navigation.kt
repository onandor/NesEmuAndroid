package com.onandor.nesemu.navigation

object NavDestinations {
    const val BACK = ""
    const val LIBRARY_SCREEN = "libraryScreen"
    const val GAME_SCREEN = "gameScreen"
    const val DEBUG_SCREEN = "debugScreen"
    const val PREFERENCES_SCREEN = "preferencesScreen"
}

object NavActions {
    fun library() = object : NavAction {
        override val destination: String = NavDestinations.LIBRARY_SCREEN
    }

    fun back() = object : NavAction {
        override val destination: String = NavDestinations.BACK
    }

    fun gameScreen() = object : NavAction {
        override val destination: String = NavDestinations.GAME_SCREEN
    }

    fun debugScreen() = object : NavAction {
        override val destination: String = NavDestinations.DEBUG_SCREEN
    }

    fun preferencesScreen() = object : NavAction {
        override val destination: String = NavDestinations.PREFERENCES_SCREEN
    }
}