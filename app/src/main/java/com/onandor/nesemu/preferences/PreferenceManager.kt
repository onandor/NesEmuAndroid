package com.onandor.nesemu.preferences

import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.input.NesInputDevice
import com.onandor.nesemu.input.NesInputDeviceType
import com.onandor.nesemu.preferences.proto.InputDevicePref
import com.onandor.nesemu.preferences.proto.InputDevicePref.InputDeviceTypePref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class PreferenceManager @Inject constructor(
    private val prefStore: ProtoPreferenceStore,
    @IODispatcher private val coroutineScope: CoroutineScope
) {

    fun observeInputPreferences() = prefStore.observe().map { it.inputPreferences }

    suspend fun getController1Device() = prefStore.observe()
        .map { it.inputPreferences.controller1Device.toNesDevice() }
        .first()

    suspend fun getController2Device() = prefStore.observe()
        .map { it.inputPreferences.controller2Device.toNesDevice() }
        .first()

    fun updateControllerDevices(device1: NesInputDevice?, device2: NesInputDevice?) {
        coroutineScope.launch {
            prefStore.updateControllerDevices(device1.toPrefDevice(), device2.toPrefDevice())
        }
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