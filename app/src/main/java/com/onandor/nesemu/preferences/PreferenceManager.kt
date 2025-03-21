package com.onandor.nesemu.preferences

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.onandor.nesemu.input.NesButton
import com.onandor.nesemu.input.NesInputDevice
import com.onandor.nesemu.input.NesInputDeviceType
import com.onandor.nesemu.input.NesInputManager
import com.onandor.nesemu.input.NesInputManager.ButtonMapKey
import com.onandor.nesemu.preferences.proto.InputDevicePref
import com.onandor.nesemu.preferences.proto.InputDevicePref.InputDeviceTypePref
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PreferenceManager @Inject constructor(
    private val prefStore: ProtoPreferenceStore
) {

    fun observeInputPreferences() = prefStore.observe().map { it.inputPreferences }

    suspend fun getController1Device() = prefStore.observe()
        .map { it.inputPreferences.controller1Device.toNesDevice() }
        .first()

    suspend fun getController2Device() = prefStore.observe()
        .map { it.inputPreferences.controller2Device.toNesDevice() }
        .first()

    suspend fun getButtonMappings() = prefStore.observe()
        .map {
            mapOf(
                ButtonMapKey(NesInputManager.PLAYER_1, NesInputDeviceType.CONTROLLER)
                        to it.inputPreferences.player1ControllerMappingMap.toButtonBiMap(),
                ButtonMapKey(NesInputManager.PLAYER_1, NesInputDeviceType.KEYBOARD)
                        to it.inputPreferences.player1KeyboardMappingMap.toButtonBiMap(),
                ButtonMapKey(NesInputManager.PLAYER_2, NesInputDeviceType.CONTROLLER)
                        to it.inputPreferences.player2ControllerMappingMap.toButtonBiMap(),
                ButtonMapKey(NesInputManager.PLAYER_2, NesInputDeviceType.KEYBOARD)
                        to it.inputPreferences.player2KeyboardMappingMap.toButtonBiMap()
            )
        }
        .first()

    suspend fun updateInputDevices(device1: NesInputDevice?, device2: NesInputDevice?) {
        prefStore.updateInputDevices(device1.toPrefDevice(), device2.toPrefDevice())
    }

    suspend fun updateButtonMappings(mappings: Map<ButtonMapKey, BiMap<Int, NesButton>>) {
        val prefMappings = mappings.mapValues { (_, biMap) ->
            biMap.mapValues { (_, button) -> button.ordinal }
        }
        prefStore.updateButtonMappings(
            prefMappings[ButtonMapKey(NesInputManager.PLAYER_1, NesInputDeviceType.CONTROLLER)]!!,
            prefMappings[ButtonMapKey(NesInputManager.PLAYER_1, NesInputDeviceType.KEYBOARD)]!!,
            prefMappings[ButtonMapKey(NesInputManager.PLAYER_2, NesInputDeviceType.CONTROLLER)]!!,
            prefMappings[ButtonMapKey(NesInputManager.PLAYER_2, NesInputDeviceType.KEYBOARD)]!!
        )
    }

    private fun NesInputDevice?.toPrefDevice(): InputDevicePref? {
        if (this == null) {
            return null
        }
        return InputDevicePref.newBuilder().run {
            name = this@toPrefDevice.name
            descriptor = this@toPrefDevice.descriptor
            type = NES_TO_PREF_DEVICE_TYPE_MAP[this@toPrefDevice.type]
            build()
        }
    }

    private fun InputDevicePref?.toNesDevice(): NesInputDevice? {
        if (this == null || this.isEmpty()) {
            return null
        }
        return NesInputDevice(
            name = this.name,
            id = null,
            descriptor = this.descriptor,
            type = PREF_TO_NES_DEVICE_TYPE_MAP[this.type]!!
        )
    }

    private fun InputDevicePref.isEmpty(): Boolean {
        return this.name == ""
                && this.descriptor == ""
                && this.type == InputDeviceTypePref.entries[0]
    }

    private fun Map<Int, Int>.toButtonBiMap(): BiMap<Int, NesButton> {
        return HashBiMap.create<Int, NesButton>(
            this.mapValues { (_, buttonOrdinal) -> NesButton.entries[buttonOrdinal] }
        )
    }

    companion object {
        private val NES_TO_PREF_DEVICE_TYPE_MAP = mapOf<NesInputDeviceType, InputDeviceTypePref>(
            NesInputDeviceType.VIRTUAL_CONTROLLER to InputDeviceTypePref.VIRTUAL_CONTROLLER,
            NesInputDeviceType.CONTROLLER to InputDeviceTypePref.CONTROLLER,
            NesInputDeviceType.KEYBOARD to InputDeviceTypePref.KEYBOARD
        )
        private val PREF_TO_NES_DEVICE_TYPE_MAP = mapOf<InputDeviceTypePref, NesInputDeviceType>(
            InputDeviceTypePref.VIRTUAL_CONTROLLER to NesInputDeviceType.VIRTUAL_CONTROLLER,
            InputDeviceTypePref.CONTROLLER to NesInputDeviceType.CONTROLLER,
            InputDeviceTypePref.KEYBOARD to NesInputDeviceType.KEYBOARD
        )
    }
}