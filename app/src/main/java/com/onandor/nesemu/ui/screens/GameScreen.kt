package com.onandor.nesemu.ui.screens

import android.content.res.Configuration
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.ui.components.NesRenderer
import com.onandor.nesemu.ui.components.NesSurfaceView
import com.onandor.nesemu.input.NesButton
import com.onandor.nesemu.ui.components.controls.DPad
import com.onandor.nesemu.ui.components.controls.FaceButton
import com.onandor.nesemu.ui.components.controls.OptionButton
import com.onandor.nesemu.ui.util.HideSystemBars
import com.onandor.nesemu.viewmodels.GameViewModel
import com.onandor.nesemu.viewmodels.GameViewModel.Event
import com.onandor.nesemu.R
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.ui.components.ListItem
import com.onandor.nesemu.ui.components.TitleDialog

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Game(
                renderer = viewModel.renderer,
                padding = padding,
                emulationPaused = uiState.emulationPaused,
                onEvent = viewModel::onEvent
            )
        }
    }

    if (uiState.errorMessage != null) {
        LaunchedEffect(uiState.errorMessage) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(Event.OnErrorMessageToastShown)
        }
    }

    if (uiState.showPauseMenu) {
        PauseMenuDialog(onEvent = viewModel::onEvent)
    }

    BackHandler {
        viewModel.onEvent(Event.OnNavigateBack)
    }
}

@Composable
private fun Game(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    renderer: NesRenderer,
    emulationPaused: Boolean,
    onEvent: (Event) -> Unit
) {
    val configuration = LocalConfiguration.current

    HideSystemBars()
    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(top = 30.dp, bottom = 30.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(256f / 240f)
            ) {
                NesSurfaceView(
                    modifier = modifier.fillMaxWidth().aspectRatio(256f / 240f),
                    renderer = renderer,
                    onRenderCallbackCreated = { onEvent(Event.OnRenderCallbackCreated(it)) }
                )
                if (emulationPaused) {
                    PauseIcon()
                }
            }
            VerticalControls(
                onEvent = onEvent
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.padding(start = 10.dp, end = 20.dp)) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onEvent(Event.OnShowPauseMenuDialog) }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalControlsLeft(
                modifier = Modifier.weight(1f),
                onEvent = onEvent
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(256f / 240f)
            ) {
                NesSurfaceView(
                    modifier = modifier.fillMaxHeight().aspectRatio(256f / 240f),
                    renderer = renderer,
                    onRenderCallbackCreated = { onEvent(Event.OnRenderCallbackCreated(it)) }
                )
                if (emulationPaused) {
                    PauseIcon()
                }
            }
            HorizontalControlsRight(
                modifier = Modifier.weight(1f),
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun VerticalControls(
    modifier: Modifier = Modifier,
    onEvent: (Event) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 25.dp, end = 25.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            DPad(onStateChanged = { onEvent(Event.OnDpadStateChanged(it)) })
            Row {
                FaceButton(
                    text = "B",
                    onStateChanged = { onEvent(Event.OnButtonStateChanged(NesButton.B, it)) }
                )
                Spacer(modifier = Modifier.width(15.dp))
                FaceButton(
                    text = "A",
                    onStateChanged = { onEvent(Event.OnButtonStateChanged(NesButton.A, it)) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OptionButton(
                text = "SELECT",
                onStateChanged = { onEvent(Event.OnButtonStateChanged(NesButton.SELECT, it)) }
            )
            Spacer(modifier = Modifier.width(20.dp))
            OptionButton(
                text = "START",
                onStateChanged = { onEvent(Event.OnButtonStateChanged(NesButton.START, it)) }
            )
        }
    }
}

@Composable
private fun HorizontalControlsLeft(
    modifier: Modifier = Modifier,
    onEvent: (Event) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        DPad(
            modifier = Modifier.align(Alignment.Center),
            onStateChanged = { onEvent(Event.OnDpadStateChanged(it)) }
        )
        OptionButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp),
            text = "SELECT",
            onStateChanged = { onEvent(Event.OnButtonStateChanged(NesButton.SELECT, it)) }
        )
    }
}

@Composable
private fun HorizontalControlsRight(
    modifier: Modifier = Modifier,
    onEvent: (Event) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 20.dp)
        ) {
            IconButton(onClick = { onEvent(Event.OnShowPauseMenuDialog) }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .height(140.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            FaceButton(
                text = "B",
                onStateChanged = { onEvent(Event.OnButtonStateChanged(NesButton.B, it)) }
            )
            Spacer(modifier = Modifier.width(15.dp))
            FaceButton(
                text = "A",
                onStateChanged = { onEvent(Event.OnButtonStateChanged(NesButton.A, it)) }
            )
        }
        OptionButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp),
            text = "START",
            onStateChanged = { onEvent(Event.OnButtonStateChanged(NesButton.START, it)) }
        )
    }
}

@Composable
private fun PauseMenuDialog(
    onEvent: (Event) -> Unit
) {
    TitleDialog(
        modifier = Modifier.onKeyEvent {
            val event = it.nativeKeyEvent
            if (event.action == KeyEvent.ACTION_UP
                && (event.keyCode == KeyEvent.KEYCODE_ESCAPE
                        || event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE)) {
                onEvent(Event.OnHidePauseMenuDialog)
            }
            true
        },
        text = "Menu",
        onDismissRequest = { onEvent(Event.OnHidePauseMenuDialog) }
    ) {
        ListItem(
            onClick = { onEvent(Event.OnHidePauseMenuDialog) },
            mainText = { Text("Resume") }
        )
        HorizontalDivider()
        ListItem(
            onClick = { onEvent(Event.OnNavigateTo(NavActions.preferencesScreen())) },
            mainText = { Text("Preferences") }
        )
        ListItem(
            onClick = { onEvent(Event.OnNavigateTo(NavActions.debugScreen())) },
            mainText = { Text("Debug view") }
        )
        HorizontalDivider()
        ListItem(
            onClick = { onEvent(Event.OnResetConsole) },
            mainText = { Text("Reset console") }
        )
        ListItem(
            onClick = { onEvent(Event.OnNavigateBack) },
            mainText = { Text("Quit game") }
        )
    }
}

@Composable
private fun PauseIcon() {
    Icon(
        modifier = Modifier
            .padding(10.dp)
            .size(65.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(5.dp),
        painter = painterResource(R.drawable.ic_pause),
        contentDescription = null,
        tint = Color.White
    )
}