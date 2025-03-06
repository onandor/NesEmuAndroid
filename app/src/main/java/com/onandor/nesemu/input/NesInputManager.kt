package com.onandor.nesemu.input

import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent

class NesInputManager(private val inputManager: InputManager) {

    private val availableDevicesMap = mutableMapOf<Int, NesInputDevice>()
    val availableDevices: List<NesInputDevice>
        get() {
            return availableDevicesMap.values.toList()
        }

    private val controller1Buttons = initControllerButtons()
    private val controller2Buttons = initControllerButtons()
    var controller1Device: NesInputDevice? = null
        private set
    var controller2Device: NesInputDevice? = null
        private set

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
        controller1Device = VIRTUAL_CONTROLLER
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
            descriptor = device.descriptor,
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

    fun setInputDevice(controllerId: Int, device: NesInputDevice) {
        if (!availableDevices.contains(device)) {
            return
        }

        if (controllerId == CONTROLLER_1) {
            controller1Device = device
            if (controller2Device == device) {
                controller2Device = null
            }
        } else {
            controller2Device = device
            if (controller1Device == device) {
                controller1Device = null
            }
        }
    }

    fun onInputEvents(deviceId: Int, buttonStates: Map<NesButton, NesButtonState>) {
        controller1Device?.let {
            if (it.id == deviceId) {
                controller1Buttons.putAll(buttonStates)
            }
        }
        controller2Device?.let {
            if (it.id == deviceId) {
                controller2Buttons.putAll(buttonStates)
            }
        }
    }

    fun onInputEvent(deviceId: Int, button: NesButton, state: NesButtonState) {
        controller1Device?.let {
            if (it.id == deviceId) {
                controller1Buttons[button] = state
            }
        }
        controller2Device?.let {
            if (it.id == deviceId) {
                controller2Buttons[button] = state
            }
        }
    }

    fun onInputEvent(event: KeyEvent): Boolean {
        val button = CONTROLLER_BUTTON_MAP[event.keyCode]
        val state = BUTTON_STATE_MAP[event.action]
        if (button == null || state == null) {
            return false
        }
        onInputEvent(event.deviceId, button, state)
        return true
    }

    fun registerListener() {
        inputManager.registerInputDeviceListener(inputDeviceListener, null)
        refreshAvailableDevices()
    }

    fun unregisterListener() {
        inputManager.unregisterInputDeviceListener(inputDeviceListener)
        availableDevicesMap.clear()
    }

    fun getButtonStates(controllerId: Int): Int {
        val buttonStateMap = if (controllerId == CONTROLLER_1) {
            controller1Buttons
        } else {
            controller2Buttons
        }

        var buttonStates = 0
        buttonStateMap.forEach { _, state ->
            buttonStates = (buttonStates shl 1) or state.ordinal
        }
        return buttonStates
    }

    private fun initControllerButtons(): MutableMap<NesButton, NesButtonState> {
        return mutableMapOf<NesButton, NesButtonState>(
            NesButton.DPAD_RIGHT to NesButtonState.UP,
            NesButton.DPAD_LEFT to NesButtonState.UP,
            NesButton.DPAD_DOWN to NesButtonState.UP,
            NesButton.DPAD_UP to NesButtonState.UP,
            NesButton.START to NesButtonState.UP,
            NesButton.SELECT to NesButtonState.UP,
            NesButton.B to NesButtonState.UP,
            NesButton.A to NesButtonState.UP
        )
    }

    companion object {
        private const val TAG = "NesInputManager"

        const val CONTROLLER_1: Int = 1
        const val CONTROLLER_2: Int = 2

        const val VIRTUAL_CONTROLLER_DEVICE_ID = Int.MIN_VALUE
        const val VIRTUAL_CONTROLLER_DEVICE_DESCRIPTOR = "VIRTUAL_CONTROLLER"
        private val VIRTUAL_CONTROLLER = NesInputDevice(
            name = "Virtual controller",
            id = VIRTUAL_CONTROLLER_DEVICE_ID,
            descriptor = VIRTUAL_CONTROLLER_DEVICE_DESCRIPTOR,
            type = NesInputDeviceType.VIRTUAL_CONTROLLER
        )

        private val CONTROLLER_BUTTON_MAP = mapOf<Int, NesButton>(
            KeyEvent.KEYCODE_DPAD_RIGHT to NesButton.DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT to NesButton.DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN to NesButton.DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_UP to NesButton.DPAD_UP,
            KeyEvent.KEYCODE_BUTTON_START to NesButton.START,
            KeyEvent.KEYCODE_BUTTON_SELECT to NesButton.SELECT,
            KeyEvent.KEYCODE_BUTTON_B to NesButton.A,
            KeyEvent.KEYCODE_BUTTON_A to NesButton.B
        )

        private val BUTTON_STATE_MAP = mapOf<Int, NesButtonState>(
            KeyEvent.ACTION_UP to NesButtonState.UP,
            KeyEvent.ACTION_DOWN to NesButtonState.DOWN
        )
    }
}