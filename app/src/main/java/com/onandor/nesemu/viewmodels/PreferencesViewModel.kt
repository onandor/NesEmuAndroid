package com.onandor.nesemu.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.collect.BiMap
import com.onandor.nesemu.domain.input.NesButton
import com.onandor.nesemu.domain.input.NesInputDevice
import com.onandor.nesemu.domain.input.NesInputDeviceType
import com.onandor.nesemu.domain.service.InputService
import com.onandor.nesemu.domain.service.InputService.ButtonMapKey
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.domain.service.LibraryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val inputManager: InputService,
    private val libraryService: LibraryService
) : ViewModel() {

    data class UiState(
        // Library
        val libraryDirectory: String = "",

        // Input device selection
        val availableDevices: List<NesInputDevice> = emptyList(),
        val player1Device: NesInputDevice? = null,
        val player2Device: NesInputDevice? = null,
        val deviceSelectionPlayerId: Int? = null,

        // Button mapping
        val controllerDropdownExpanded: Boolean = false,
        val inputDeviceDropdownExpanded: Boolean = false,
        val buttonMappingPlayerId: Int = InputService.PLAYER_1,
        val buttonMappingDeviceType: NesInputDeviceType = NesInputDeviceType.Controller,
        val editedButton: NesButton? = null,
        val displayedButtonMapping: Map<NesButton, Int> = emptyMap()
    )

    sealed class Event {
        // Library
        data class OnNewLibrarySelected(val libraryUri: String) : Event()

        // Input device selection
        data class OnOpenDeviceSelectionDialog(val playerId: Int) : Event()
        object OnCloseDeviceSelectionDialog : Event()
        data class OnDeviceSelected(val playerId: Int, val device: NesInputDevice?) : Event()

        // Button mapping
        data class OnControllerDropdownStateChanged(val expanded: Boolean) : Event()
        data class OnInputDeviceDropdownStateChanged(val expanded: Boolean) : Event()
        data class OnButtonMappingPlayerIdChanged(val playerId: Int) : Event()
        data class OnButtonMappingDeviceTypeChanged(val deviceType: NesInputDeviceType) : Event()
        data class OnShowEditButtonDialog(val button: NesButton) : Event()
        object OnHideEditButtonDialog : Event()
        data class OnUpdateEditedButton(val keyCode: Int) : Event()

        object OnNavigateBack : Event()
    }

    private var buttonMappings: Map<ButtonMapKey, BiMap<Int, NesButton>> = emptyMap()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = combine(
        _uiState, inputManager.state, libraryService.state
    ) { uiState, inputManagerState, libraryServiceState ->
        buttonMappings = inputManagerState.buttonMappings
        val buttonMapKey = ButtonMapKey(
            playerId = uiState.buttonMappingPlayerId,
            inputDeviceType = uiState.buttonMappingDeviceType
        )

        uiState.copy(
            libraryDirectory = libraryServiceState.libraryDirectory?.name ?: "<no folder selected>",
            availableDevices = inputManagerState.availableDevices,
            player1Device = inputManagerState.controller1Device,
            player2Device = inputManagerState.controller2Device,
            displayedButtonMapping = buttonMappings[buttonMapKey]!!.inverse().toMap()
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2000),
            initialValue = UiState()
        )

    fun onEvent(event: Event) {
        when (event) {
            // Library
            is Event.OnNewLibrarySelected -> {
                viewModelScope.launch { libraryService.changeLibraryUri(event.libraryUri) }
            }

            // Input device selection
            is Event.OnOpenDeviceSelectionDialog -> {
                updateSelectedPlayerId(event.playerId)
            }
            is Event.OnCloseDeviceSelectionDialog -> {
                updateSelectedPlayerId(null)
            }
            is Event.OnDeviceSelected -> {
                inputManager.setInputDevice(event.playerId, event.device)
                updateSelectedPlayerId(null)
            }

            // Button mapping
            is Event.OnControllerDropdownStateChanged -> {
                _uiState.update { it.copy(controllerDropdownExpanded = event.expanded) }
            }
            is Event.OnInputDeviceDropdownStateChanged -> {
                _uiState.update { it.copy(inputDeviceDropdownExpanded = event.expanded) }
            }
            is Event.OnButtonMappingPlayerIdChanged -> {
                _uiState.update {
                    it.copy(
                        buttonMappingPlayerId = event.playerId,
                        controllerDropdownExpanded = false
                    )
                }
            }
            is Event.OnButtonMappingDeviceTypeChanged -> {
                _uiState.update {
                    it.copy(
                        buttonMappingDeviceType = event.deviceType,
                        inputDeviceDropdownExpanded = false
                    )
                }
            }
            is Event.OnShowEditButtonDialog -> {
                _uiState.update { it.copy(editedButton = event.button) }
            }
            is Event.OnHideEditButtonDialog -> {
                _uiState.update { it.copy(editedButton = null) }
            }
            is Event.OnUpdateEditedButton -> {
                inputManager.changeButtonMapping(
                    event.keyCode,
                    _uiState.value.editedButton!!,
                    _uiState.value.buttonMappingPlayerId,
                    _uiState.value.buttonMappingDeviceType
                )
                _uiState.update { it.copy(editedButton = null) }
            }

            is Event.OnNavigateBack -> {
                navManager.navigateBack()
            }
        }
    }

    private fun updateSelectedPlayerId(playerId: Int?) {
        _uiState.update { it.copy(deviceSelectionPlayerId = playerId) }
    }
}