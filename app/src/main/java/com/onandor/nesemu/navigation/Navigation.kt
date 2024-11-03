package com.onandor.nesemu.navigation

private object Screens {
    const val MAIN_SCREEN = "mainScreen"
    const val GAME_SCREEN = "gameScreen"
    const val DEBUG_SCREEN = "debugScreen"
}

object NavDestinations {
    const val BACK = ""
    const val MAIN_SCREEN = Screens.MAIN_SCREEN
    const val GAME_SCREEN = Screens.GAME_SCREEN
    const val DEBUG_SCREEN = Screens.DEBUG_SCREEN
}

object NavActions {
    fun main() = object : NavAction {
        override val destination: String = NavDestinations.MAIN_SCREEN
    }

    fun back() = object : NavAction {
        override val destination: String = NavDestinations.BACK
    }

    fun gameScreen(cartridgeNavArgs: CartridgeNavArgs) = object : NavAction {
        override val destination: String = NavDestinations.GAME_SCREEN
        override val navArgs: NavArgs = cartridgeNavArgs
    }

    fun debugScreen() = object : NavAction {
        override val destination: String = NavDestinations.DEBUG_SCREEN
    }
}