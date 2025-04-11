package com.onandor.nesemu.domain.service

import android.view.KeyEvent
import com.google.common.collect.BiMap
import com.onandor.nesemu.domain.input.NesButton
import com.onandor.nesemu.domain.input.NesButtonState
import com.onandor.nesemu.domain.input.NesInputDevice
import com.onandor.nesemu.domain.input.NesInputDeviceType
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface InputService {

    data class ButtonMapKey(val playerId: Int, val inputDeviceType: NesInputDeviceType)

    data class State(
        val availableDevices: List<NesInputDevice> = emptyList(),
        val controller1Device: NesInputDevice? = null,
        val controller2Device: NesInputDevice? = null,
        val buttonMappings: Map<ButtonMapKey, BiMap<Int, NesButton>> = emptyMap()
    )

    sealed class Event {
        data object OnPauseButtonPressed : Event()
        data class OnInputDeviceDisconnected(val playerId: Int) : Event()
    }

    val state: StateFlow<State>
    val events: SharedFlow<Event>

    fun changeInputDevice(playerId: Int, device: NesInputDevice?)
    fun onInputEvents(deviceId: Int, buttonStates: Map<NesButton, NesButtonState>)
    fun onInputEvent(deviceId: Int, button: NesButton, state: NesButtonState)
    fun onInputEvent(event: KeyEvent): Boolean
    fun getButtonStates(playerId: Int): Int
    fun changeButtonMapping(
        keyCode: Int,
        button: NesButton,
        playerId: Int,
        deviceType: NesInputDeviceType
    )

    companion object {
        const val PLAYER_1: Int = 1
        const val PLAYER_2: Int = 2

        const val VIRTUAL_CONTROLLER_DEVICE_ID = Int.MIN_VALUE
    }
}