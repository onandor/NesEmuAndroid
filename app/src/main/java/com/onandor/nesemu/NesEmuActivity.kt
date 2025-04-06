package com.onandor.nesemu

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.onandor.nesemu.data.preferences.PreferenceManager
import com.onandor.nesemu.domain.service.InputService
import com.onandor.nesemu.navigation.NavGraph
import com.onandor.nesemu.ui.theme.NesEmuTheme
import com.onandor.nesemu.util.GlobalLifecycleObserver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NesEmuActivity : ComponentActivity() {

    @Inject lateinit var inputService: InputService
    @Inject lateinit var lifecycleObserver: GlobalLifecycleObserver
    @Inject lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(lifecycleObserver)

        setContent {
            val useDarkTheme by prefManager.observeUseDarkTheme().collectAsState(initial = false)

            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = android.graphics.Color.TRANSPARENT,
                        darkScrim = android.graphics.Color.TRANSPARENT,
                        detectDarkMode = { useDarkTheme }
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = android.graphics.Color.TRANSPARENT,
                        darkScrim = android.graphics.Color.TRANSPARENT,
                        detectDarkMode = { useDarkTheme }
                    )
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose {}
            }

            NesEmuTheme(darkTheme = useDarkTheme) {
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
        return if (inputService.onInputEvent(event)) {
            true
        } else {
            super.dispatchKeyEvent(event)
        }
    }
}
