package com.onandor.nesemu

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.navigation.NavGraph
import com.onandor.nesemu.ui.theme.NesEmuTheme
import com.onandor.nesemu.util.GlobalLifecycleObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NesEmuActivity : ComponentActivity() {

    @Inject lateinit var inputManager: NesInputManager
    @Inject lateinit var lifecycleObserver: GlobalLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(lifecycleObserver)

        enableEdgeToEdge()
        setContent {
            NesEmuTheme {
                NavGraph()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(lifecycleObserver)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (inputManager.onInputEvent(event)) {
            true
        } else {
            super.dispatchKeyEvent(event)
        }
    }
}
