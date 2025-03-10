package com.onandor.nesemu.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.nesemu.input.NesInputDevice
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.util.PreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
        data class OnDeviceSelected(val controllerId: Int, val device: NesInputDevice) : Event()
        object OnNavigateBack : Event()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = combine(
        _uiState, inputManager.state
    ) { uiState, inputManagerState ->
        uiState.copy(
            availableDevices = inputManagerState.availableDevices,
            controller1Device = inputManagerState.controller1Device,
            controller2Device = inputManagerState.controller2Device
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2000),
            initialValue = UiState()
        )

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnOpenDeviceSelectionDialog -> {
                _uiState.update { it.copy(deviceSelectionControllerId = event.controllerId) }
            }
            is Event.OnCloseDeviceSelectionDialog -> {
                _uiState.update { it.copy(deviceSelectionControllerId = null) }
            }
            is Event.OnDeviceSelected -> {
                inputManager.setInputDevice(event.controllerId, event.device)
                _uiState.update { it.copy(deviceSelectionControllerId = null) }
            }
            is Event.OnNavigateBack -> {
                navManager.navigateBack()
            }
        }
    }
}