package com.onandor.nesemu.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.nesemu.input.NesInputDevice
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.util.PreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val prefs: PreferenceStore,
    private val inputManager: NesInputManager
) : ViewModel() {

    data class UiState(
        val availableDevices: List<NesInputDevice> = emptyList(),
        val controller1Device: NesInputDevice? = null,
        val controller2Device: NesInputDevice? = null,
        val deviceSelectionControllerId: Int? = null
    )

    sealed class Event {
        data class OnOpenDeviceSelectionDialog(val controllerId: Int) : Event()
        object OnCloseDeviceSelectionDialog : Event()
        object OnRefreshInputDevices : Event()
        data class OnDeviceSelected(val controllerId: Int, val device: NesInputDevice) : Event()
        object OnNavigateBack : Event()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                availableDevices = inputManager.availableDevices,
                controller1Device = inputManager.controller1Device,
                controller2Device = inputManager.controller2Device
            )
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnOpenDeviceSelectionDialog -> {
                _uiState.update { it.copy(deviceSelectionControllerId = event.controllerId) }
            }
            is Event.OnCloseDeviceSelectionDialog -> {
                _uiState.update { it.copy(deviceSelectionControllerId = null) }
            }
            is Event.OnRefreshInputDevices -> {
                inputManager.refreshAvailableDevices()
            }
            is Event.OnDeviceSelected -> {
                inputManager.setInputDevice(event.controllerId, event.device)
                if (event.controllerId == NesInputManager.CONTROLLER_1) {
                    _uiState.update {
                        it.copy(
                            controller1Device = inputManager.controller1Device,
                            deviceSelectionControllerId = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            controller2Device = inputManager.controller2Device,
                            deviceSelectionControllerId = null
                        )
                    }
                }
            }
            is Event.OnNavigateBack -> {
                navManager.navigateBack()
            }
        }
    }
}