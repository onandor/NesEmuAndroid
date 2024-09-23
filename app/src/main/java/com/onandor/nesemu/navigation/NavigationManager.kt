package com.onandor.nesemu.navigation

import kotlinx.coroutines.flow.SharedFlow

interface NavigationManager {

    val navActions: SharedFlow<NavAction?>
    fun navigateTo(navAction: NavAction?)
    fun navigateBack()
    fun getCurrentRoute(): String
    fun getCurrentNavAction(): NavAction?
}