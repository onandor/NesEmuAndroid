package com.onandor.nesemu.navigation

import androidx.navigation.NavOptions

interface NavAction {

    val destination: String
    val navOptions: NavOptions
        get() = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()
    val navArgs: NavArgs?
        get() = null
}