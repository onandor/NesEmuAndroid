package com.onandor.nesemu.ui.screens

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.input.NesInputDevice
import com.onandor.nesemu.input.NesInputDeviceType
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.ui.components.ListItem
import com.onandor.nesemu.viewmodels.PreferencesViewModel
import com.onandor.nesemu.viewmodels.PreferencesViewModel.Event
import com.onandor.nesemu.R
import com.onandor.nesemu.input.ButtonMapping
import com.onandor.nesemu.input.NesButton
import com.onandor.nesemu.ui.components.ListDropdownMenu
import com.onandor.nesemu.ui.components.StatusBarScaffold
import com.onandor.nesemu.ui.components.TitleDialog
import com.onandor.nesemu.ui.components.TopBar

@Composable
fun PreferencesScreen(
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    StatusBarScaffold(
        topBar = { TopBar { viewModel.onEvent(Event.OnNavigateBack) } }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            InputDeviceSelection(
                controller1Device = uiState.player1Device,
                controller2Device = uiState.player2Device,
                onEvent = viewModel::onEvent
            )
            HorizontalDivider()
            ButtonMapping(
                playerId = uiState.buttonMappingPlayerId,
                inputDeviceType = uiState.buttonMappingDeviceType,
                controllerDropdownExpanded = uiState.controllerDropdownExpanded,
                inputDeviceDropdownExpanded = uiState.inputDeviceDropdownExpanded,
                currentMapping = uiState.displayedButtonMapping,
                onEvent = viewModel::onEvent
            )
        }
    }

    if (uiState.deviceSelectionPlayerId != null) {
        DeviceSelectionDialog(
            playerId = uiState.deviceSelectionPlayerId!!,
            availableDevices = uiState.availableDevices,
            onEvent = viewModel::onEvent
        )
    }

    if (uiState.editedButton != null) {
        EditButtonMappingDialog(
            onEvent = viewModel::onEvent
        )
    }

    BackHandler {
        viewModel.onEvent(Event.OnNavigateBack)
    }
}

@Composable
private fun InputDeviceSelection(
    controller1Device: NesInputDevice?,
    controller2Device: NesInputDevice?,
    onEvent: (Event) -> Unit
) {
    ListItem(
        onClick = { onEvent(Event.OnOpenDeviceSelectionDialog(NesInputManager.PLAYER_1)) },
        mainText = {
            Text(
                text = "Controller 1",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        subText = {
            InputDeviceText(
                deviceName = controller1Device?.name ?: "not connected",
                available = controller1Device == null || controller1Device.id != null
            )
        },
        displayItem = {
            InputDeviceIcon(
                modifier = Modifier.size(35.dp),
                device = controller1Device
            )
        }
    )
    ListItem(
        onClick = { onEvent(Event.OnOpenDeviceSelectionDialog(NesInputManager.PLAYER_2)) },
        mainText = {
            Text(
                text = "Controller 2",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        subText = {
            InputDeviceText(
                deviceName = controller2Device?.name ?: "not connected",
                available = controller2Device == null || controller2Device.id != null
            )
        },
        displayItem = {
            InputDeviceIcon(
                modifier = Modifier.size(35.dp),
                device = controller2Device
            )
        }
    )
}

@Composable
private fun ButtonMapping(
    playerId: Int,
    inputDeviceType: NesInputDeviceType,
    controllerDropdownExpanded: Boolean,
    inputDeviceDropdownExpanded: Boolean,
    currentMapping: Map<NesButton, Int>,
    onEvent: (Event) -> Unit
) {
    Image(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        painter = painterResource(R.drawable.nes_controller),
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        contentScale = ContentScale.FillWidth
    )
    ListItem(
        onClick = { onEvent(Event.OnControllerDropdownStateChanged(true)) },
        mainText = {
            Text(
                text = "NES controller",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        subText = {
            val text = if (playerId == NesInputManager.PLAYER_1) {
                "Player 1"
            } else {
                "Player 2"
            }
            Text(text)
        },
        displayItem = {
            ListDropdownMenu(
                expanded = controllerDropdownExpanded,
                onDismissRequest = { onEvent(Event.OnControllerDropdownStateChanged(false)) }
            ) {
                DropdownMenuItem(
                    text = { Text("Player 1") },
                    onClick = {
                        onEvent(Event.OnButtonMappingPlayerIdChanged(NesInputManager.PLAYER_1))
                    }
                )
                DropdownMenuItem(
                    text = { Text("Player 2") },
                    onClick = {
                        onEvent(Event.OnButtonMappingPlayerIdChanged(NesInputManager.PLAYER_2))
                    }
                )
            }
        }
    )
    ListItem(
        onClick = { onEvent(Event.OnInputDeviceDropdownStateChanged(true)) },
        mainText = {
            Text(
                text = "Input device type",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        subText = {
            val text = if (inputDeviceType == NesInputDeviceType.CONTROLLER) {
                "Controller"
            } else {
                "Keyboard"
            }
            Text(text)
        },
        displayItem = {
            ListDropdownMenu(
                expanded = inputDeviceDropdownExpanded,
                onDismissRequest = { onEvent(Event.OnInputDeviceDropdownStateChanged(false)) }
            ) {
                DropdownMenuItem(
                    text = { Text("Controller") },
                    onClick = {
                        onEvent(Event.OnButtonMappingDeviceTypeChanged(NesInputDeviceType.CONTROLLER))
                    }
                )
                DropdownMenuItem(
                    text = { Text("Keyboard") },
                    onClick = {
                        onEvent(Event.OnButtonMappingDeviceTypeChanged(NesInputDeviceType.KEYBOARD))
                    }
                )
            }
        }
    )
    ButtonMappingEdit(
        inputDeviceType = inputDeviceType,
        mapping = currentMapping,
        onEvent = onEvent
    )
}

@Composable
private fun ButtonMappingEdit(
    inputDeviceType: NesInputDeviceType,
    mapping: Map<NesButton, Int>,
    onEvent: (Event) -> Unit
) {
    if (inputDeviceType == NesInputDeviceType.KEYBOARD) {
        ButtonMappingListItem(
            buttonName = "DPad up",
            button = NesButton.DPAD_UP,
            deviceType = inputDeviceType,
            mapping = mapping,
            onStartEdit = { onEvent(Event.OnShowEditButtonDialog(NesButton.DPAD_UP)) }
        )
        ButtonMappingListItem(
            buttonName = "DPad down",
            button = NesButton.DPAD_DOWN,
            deviceType = inputDeviceType,
            mapping = mapping,
            onStartEdit = { onEvent(Event.OnShowEditButtonDialog(NesButton.DPAD_DOWN)) }
        )
        ButtonMappingListItem(
            buttonName = "DPad left",
            button = NesButton.DPAD_LEFT,
            deviceType = inputDeviceType,
            mapping = mapping,
            onStartEdit = { onEvent(Event.OnShowEditButtonDialog(NesButton.DPAD_LEFT)) }
        )
        ButtonMappingListItem(
            buttonName = "DPad right",
            button = NesButton.DPAD_RIGHT,
            deviceType = inputDeviceType,
            mapping = mapping,
            onStartEdit = { onEvent(Event.OnShowEditButtonDialog(NesButton.DPAD_RIGHT)) }
        )
    }
    ButtonMappingListItem(
        buttonName = "Button \"A\"",
        button = NesButton.A,
        deviceType = inputDeviceType,
        mapping = mapping,
        onStartEdit = { onEvent(Event.OnShowEditButtonDialog(NesButton.A)) }
    )
    ButtonMappingListItem(
        buttonName = "Button \"B\"",
        button = NesButton.B,
        deviceType = inputDeviceType,
        mapping = mapping,
        onStartEdit = { onEvent(Event.OnShowEditButtonDialog(NesButton.B)) }
    )
    ButtonMappingListItem(
        buttonName = "Button \"START\"",
        button = NesButton.START,
        deviceType = inputDeviceType,
        mapping = mapping,
        onStartEdit = { onEvent(Event.OnShowEditButtonDialog(NesButton.START)) }
    )
    ButtonMappingListItem(
        buttonName = "Button \"SELECT\"",
        button = NesButton.SELECT,
        deviceType = inputDeviceType,
        mapping = mapping,
        onStartEdit = { onEvent(Event.OnShowEditButtonDialog(NesButton.SELECT)) }
    )
}

@Composable
private fun ButtonMappingListItem(
    buttonName: String,
    button: NesButton,
    deviceType: NesInputDeviceType,
    mapping: Map<NesButton, Int>,
    onStartEdit: () -> Unit
) {
    ListItem(
        modifier = Modifier.height(50.dp),
        onClick = onStartEdit,
        mainText = { Text(buttonName) },
        displayItem = {
            val iconResource = if (deviceType == NesInputDeviceType.CONTROLLER) {
                ButtonMapping.CONTROLLER_KEYCODE_ICON_MAP[mapping[button]]
            } else {
                ButtonMapping.KEYBOARD_KEYCODE_ICON_MAP[mapping[button]]
            }

            if (iconResource == null) {
                Text("unmapped")
            } else {
                Icon(
                    painter = painterResource(iconResource),
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun EditButtonMappingDialog(
    onEvent: (Event) -> Unit
) {
    TitleDialog(
        modifier = Modifier.onKeyEvent {
            val event = it.nativeKeyEvent
            if (event.action == KeyEvent.ACTION_UP) {
                onEvent(Event.OnUpdateEditedButton(event.keyCode))
            }
            true
        },
        text = "Edit button mapping",
        onDismissRequest = { onEvent(Event.OnHideEditButtonDialog) }
    ) {
        Text(
            modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 10.dp),
            text = "Press a button...",
            fontStyle = FontStyle.Italic
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onNavigateBack: () -> Unit
) {
    TopBar(
        title = "Preferences",
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, null)
            }
        }
    )
}

@Composable
private fun DeviceSelectionDialog(
    playerId: Int,
    availableDevices: List<NesInputDevice>,
    onEvent: (Event) -> Unit
) {
    TitleDialog(
        text = "Select input device",
        onDismissRequest = { onEvent(Event.OnCloseDeviceSelectionDialog) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            availableDevices.forEach { device ->
                ListItem(
                    onClick = { onEvent(Event.OnDeviceSelected(playerId, device)) },
                    mainText = { InputDeviceText(device.name, true) },
                    displayItem = { InputDeviceIcon(device = device) }
                )
            }
            HorizontalDivider()
            ListItem(
                onClick = { onEvent(Event.OnDeviceSelected(playerId, null)) },
                mainText = { InputDeviceText("None", true) }
            )
        }
    }
}

@Composable
private fun InputDeviceIcon(modifier: Modifier = Modifier, device: NesInputDevice?) {
    device?.let {
        val icon = when (it.type) {
            NesInputDeviceType.CONTROLLER -> R.drawable.ic_controller
            NesInputDeviceType.VIRTUAL_CONTROLLER -> R.drawable.ic_device
            NesInputDeviceType.KEYBOARD -> R.drawable.ic_keyboard
        }
        Icon(
            modifier = modifier,
            painter = painterResource(icon),
            contentDescription = null
        )
    }
}

@Composable
private fun InputDeviceText(deviceName: String, available: Boolean) {
    if (available) {
        Text(
            text = deviceName,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Text(
            text = deviceName,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}