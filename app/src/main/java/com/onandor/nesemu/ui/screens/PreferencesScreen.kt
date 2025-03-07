package com.onandor.nesemu.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.input.NesInputDevice
import com.onandor.nesemu.input.NesInputDeviceType
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.ui.components.ClickableListItem
import com.onandor.nesemu.viewmodels.PreferencesViewModel
import com.onandor.nesemu.viewmodels.PreferencesViewModel.Event
import com.onandor.nesemu.R

@Composable
fun PreferencesScreen(
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopBar { viewModel.onEvent(Event.OnNavigateBack) } }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ClickableListItem(
                onClick = {
                    viewModel
                        .onEvent(Event.OnOpenDeviceSelectionDialog(NesInputManager.CONTROLLER_1))
                },
                mainText = {
                    Text(
                        text = "Controller 1",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                subText = { InputDeviceText(uiState.controller1Device?.name ?: "not connected") },
                displayItem = {
                    InputDeviceIcon(
                        modifier = Modifier.size(35.dp),
                        device = uiState.controller1Device
                    )
                }
            )
            ClickableListItem(
                onClick = {
                    viewModel
                        .onEvent(Event.OnOpenDeviceSelectionDialog(NesInputManager.CONTROLLER_2))
                },
                mainText = {
                    Text(
                        text = "Controller 2",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subText = { InputDeviceText(uiState.controller2Device?.name ?: "not connected") },
                displayItem = {
                    InputDeviceIcon(
                        modifier = Modifier.size(35.dp),
                        device = uiState.controller2Device
                    )
                }
            )
        }
    }

    if (uiState.deviceSelectionControllerId != null) {
        DeviceSelectionDialog(
            controllerId = uiState.deviceSelectionControllerId!!,
            availableDevices = uiState.availableDevices,
            onEvent = { viewModel.onEvent(it) }
        )
    }

    BackHandler {
        viewModel.onEvent(Event.OnNavigateBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = { Text("Preferences") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, null)
            }
        }
    )
}

@Composable
private fun DeviceSelectionDialog(
    controllerId: Int,
    availableDevices: List<NesInputDevice>,
    onEvent: (Event) -> Unit
) {
    Dialog(onDismissRequest = { onEvent(Event.OnCloseDeviceSelectionDialog) }) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 20.dp),
                    text = "Select input device",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                availableDevices.forEach { device ->
                    ClickableListItem(
                        modifier = Modifier.height(70.dp),
                        onClick = { onEvent(Event.OnDeviceSelected(controllerId, device)) },
                        mainText = { InputDeviceText(device.name) },
                        displayItem = { InputDeviceIcon(device = device) }
                    )
                }
            }
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
private fun InputDeviceText(deviceName: String) {
    Text(
        text = deviceName,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}