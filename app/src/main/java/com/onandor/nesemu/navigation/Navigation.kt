package com.onandor.nesemu.navigation

private object Screens {
    const val MAIN_SCREEN = "mainScreen"
}

object NavDestinations {
    const val BACK = ""
    const val MAIN = Screens.MAIN_SCREEN
}

object NavActions {
    fun main() = object : NavAction {
        override val destination: String = NavDestinations.MAIN
    }

    fun back() = object : NavAction {
        override val destination: String = NavDestinations.BACK
    }
}