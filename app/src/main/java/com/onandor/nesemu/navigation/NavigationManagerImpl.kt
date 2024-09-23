package com.onandor.nesemu.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Stack

class NavigationManagerImpl : NavigationManager {

    private val mNavActions: MutableSharedFlow<NavAction?> by lazy { MutableSharedFlow() }
    override val navActions: SharedFlow<NavAction?> = mNavActions.asSharedFlow()
    private val mBackStack: Stack<NavAction> = Stack()
    private var mCurrentRoute: String = ""

    init {
        mBackStack.push(NavActions.main())
        mCurrentRoute = NavActions.main().destination
    }

    override fun navigateTo(navAction: NavAction?) {
        navAction?.let {
            if (navAction.navOptions.popUpToId == 0) {
                mBackStack.clear()
            }
            mBackStack.push(navAction)
            mCurrentRoute = navAction.destination
            CoroutineScope(Dispatchers.Main).launch {
                mNavActions.emit(navAction)
            }
        }
    }

    override fun navigateBack() {
        if (mBackStack.isEmpty()) {
            return
        }
        mBackStack.pop()
        mCurrentRoute = mBackStack.peek().destination
        CoroutineScope(Dispatchers.Main).launch {
            mNavActions.emit(NavActions.back())
        }
    }

    override fun getCurrentRoute(): String {
        return mCurrentRoute
    }

    override fun getCurrentNavAction(): NavAction? {
        if (mBackStack.isEmpty()) {
            return null
        }
        return mBackStack.peek()
    }
}