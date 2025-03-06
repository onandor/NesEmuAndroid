package com.onandor.nesemu.input

import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent

class NesInputManager(private val inputManager: InputManager) {

    private var availableDevicesMap = mutableMapOf<Int, NesInputDevice>()
    val availableDevices: List<NesInputDevice> = availableDevicesMap.values.toList()

    private val controller1Handler = InputHandler()
    private val controller2Handler = InputHandler()
    private var controller1Device: NesInputDevice? = null
    private var controller2Device: NesInputDevice? = null

    private val inputDeviceListener = object : InputManager.InputDeviceListener {

        override fun onInputDeviceAdded(deviceId: Int) {
            val device = InputDevice.getDevice(deviceId)
            if (device == null || availableDevicesMap.contains(deviceId)) {
                return
            }
            getNesInputDevice(device)?.let { availableDevicesMap.put(it.id, it) }
            Log.d(TAG, "Input device added: ${device.name} (id: $deviceId)")
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            val nesDevice = availableDevicesMap.remove(deviceId)
            Log.d(TAG, "Input device removed: ${nesDevice?.name} (id: ${nesDevice?.id})")
        }

        override fun onInputDeviceChanged(deviceId: Int) {}
    }

    init {
        refreshAvailableDevices()
    }

    private fun getNesInputDevice(device: InputDevice): NesInputDevice? {
        if (device.isVirtual || !device.isEnabled) {
            return null
        }

        val deviceType =
            if (device.supportsSource(InputDevice.SOURCE_GAMEPAD)
                || device.supportsSource(InputDevice.SOURCE_GAMEPAD)) {
                NesInputDeviceType.CONTROLLER
            } else if (device.supportsSource(InputDevice.SOURCE_KEYBOARD)) {
                NesInputDeviceType.KEYBOARD
            } else {
                return null
            }

        return NesInputDevice(
            name = device.name,
            id = device.id,
            type = deviceType
        )
    }

    fun refreshAvailableDevices() {
        availableDevicesMap.clear()
        availableDevicesMap.put(VIRTUAL_CONTROLLER.id, VIRTUAL_CONTROLLER)

        val deviceIds = InputDevice.getDeviceIds()

        for (i in 0 ..< deviceIds.size) {
            val device = InputDevice.getDevice(deviceIds[i])
            if (device == null || availableDevicesMap.contains(device.id)) {
                continue
            }

            getNesInputDevice(device)?.let { availableDevicesMap.put(it.id, it) }
        }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        val button = CONTROLLER_BUTTON_MAP[event.keyCode]
        val state = BUTTON_STATE_MAP[event.action]
        if (button == null || state == null) {
            return false
        }

        controller1Device?.let {
            if (it.id == event.deviceId) {
                controller1Handler.onKeyEvent(button, state)
            }
        }
        controller2Device?.let {
            if (it.id == event.deviceId) {
                controller2Handler.onKeyEvent(button, state)
            }
        }
        return true
    }

    fun registerListener() {
        inputManager.registerInputDeviceListener(inputDeviceListener, null)
    }

    fun unregisterListener() {
        inputManager.unregisterInputDeviceListener(inputDeviceListener)
        availableDevicesMap.clear()
    }

    companion object {
        private const val TAG = "NesInputManager"

        const val CONTROLLER_1: Int = 1
        const val CONTROLLER_2: Int = 2

        private val VIRTUAL_CONTROLLER = NesInputDevice(
            id = Int.MIN_VALUE,
            name = "Virtual controller",
            type = NesInputDeviceType.VIRTUAL_CONTROLLER
        )

        private val CONTROLLER_BUTTON_MAP = mapOf<Int, NesButton>(
            KeyEvent.KEYCODE_DPAD_RIGHT to NesButton.DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT to NesButton.DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN to NesButton.DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_UP to NesButton.DPAD_UP,
            KeyEvent.KEYCODE_BUTTON_START to NesButton.START,
            KeyEvent.KEYCODE_BUTTON_SELECT to NesButton.SELECT,
            KeyEvent.KEYCODE_BUTTON_B to NesButton.B,
            KeyEvent.KEYCODE_BUTTON_A to NesButton.A
        )

        private val BUTTON_STATE_MAP = mapOf<Int, NesButtonState>(
            KeyEvent.ACTION_UP to NesButtonState.UP,
            KeyEvent.ACTION_DOWN to NesButtonState.DOWN
        )
    }
}