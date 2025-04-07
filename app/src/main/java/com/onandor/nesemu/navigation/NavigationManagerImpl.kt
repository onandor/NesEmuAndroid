package com.onandor.nesemu.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Stack

class NavigationManagerImpl : NavigationManager {

    private val _navActions: MutableSharedFlow<NavAction?> by lazy { MutableSharedFlow() }
    override val navActions: SharedFlow<NavAction?> = _navActions.asSharedFlow()
    private val backStack: Stack<NavAction> = Stack()
    private var currentRoute: String = ""

    init {
        backStack.push(NavActions.library())
        currentRoute = NavActions.library().destination
    }

    override fun navigateTo(navAction: NavAction?) {
        navAction?.let {
            if (navAction.navOptions.popUpToId == 0) {
                backStack.clear()
            }
            backStack.push(navAction)
            currentRoute = navAction.destination
            CoroutineScope(Dispatchers.Main).launch {
                _navActions.emit(navAction)
            }
        }
    }

    override fun navigateBack() {
        if (backStack.isEmpty()) {
            return
        }
        backStack.pop()
        if (backStack.isNotEmpty()) {
            currentRoute = backStack.peek().destination
        }
        CoroutineScope(Dispatchers.Main).launch {
            _navActions.emit(NavActions.back())
        }
    }

    override fun getCurrentRoute(): String {
        return currentRoute
    }

    override fun getCurrentNavAction(): NavAction? {
        if (backStack.isEmpty()) {
            return null
        }
        return backStack.peek()
    }
}