package com.onandor.nesemu.input

import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.preferences.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NesInputManager(
    private val inputManager: InputManager,
    private val prefManager: PreferenceManager,
    @IODispatcher private val coroutineScope: CoroutineScope
) {

    data class State(
        val availableDevices: List<NesInputDevice> = emptyList(),
        val controller1Device: NesInputDevice? = null,
        val controller2Device: NesInputDevice? = null
    )

    sealed class Event {
        object OnPauseButtonPressed : Event()
    }

    private val availableDevicesMap = mutableMapOf<Int, NesInputDevice>()

    private val controller1Buttons: MutableMap<NesButton, NesButtonState> = initControllerButtons()
    private val controller2Buttons: MutableMap<NesButton, NesButtonState> = initControllerButtons()
    var controller1Device: NesInputDevice? = null
        private set
    var controller2Device: NesInputDevice? = null
        private set

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val inputDeviceListener = object : InputManager.InputDeviceListener {

        override fun onInputDeviceAdded(deviceId: Int) {
            val device = InputDevice.getDevice(deviceId)
            if (device == null || availableDevicesMap.contains(deviceId)) {
                return
            }
            createNesInputDevice(device)?.let {
                availableDevicesMap.put(it.id!!, it)
                if (controller1Device?.descriptor == it.descriptor) {
                    controller1Device = it
                } else if (controller2Device?.descriptor == it.descriptor) {
                    controller2Device = it
                }
                updateState()
            }
            Log.d(TAG, "Input device added: ${device.name} (id: $deviceId)")
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            val nesDevice = availableDevicesMap.remove(deviceId)
            nesDevice?.let {
                if (controller1Device == it) {
                    controller1Device = controller1Device?.copy(id = null)
                } else if (controller2Device == it) {
                    controller2Device = controller2Device?.copy(id = null)
                }
                updateState()
                saveState()
            }
            Log.d(TAG, "Input device removed: ${nesDevice?.name} (id: ${nesDevice?.id})")
        }

        override fun onInputDeviceChanged(deviceId: Int) {}
    }

    init {
        refreshAvailableDevices()
        loadPreferences()
    }

    private fun createNesInputDevice(device: InputDevice): NesInputDevice? {
        if (device.isVirtual || !device.isEnabled) {
            return null
        }

        val deviceType = getDeviceType(device)
        if (deviceType == null) {
            return null
        }

        return NesInputDevice(
            name = device.name,
            id = device.id,
            descriptor = device.descriptor,
            type = deviceType
        )
    }

    private fun getDeviceType(device: InputDevice): NesInputDeviceType? {
        if (device.supportsSource(InputDevice.SOURCE_KEYBOARD)
            && device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
            return NesInputDeviceType.KEYBOARD
        }
        if ((device.supportsSource(InputDevice.SOURCE_GAMEPAD)
            || device.supportsSource(InputDevice.SOURCE_JOYSTICK)
            || device.supportsSource(InputDevice.SOURCE_DPAD))
            && device.keyboardType != InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
            return NesInputDeviceType.CONTROLLER
        }

        return null
    }

    private fun refreshAvailableDevices() {
        availableDevicesMap.clear()
        availableDevicesMap.put(VIRTUAL_CONTROLLER.id!!, VIRTUAL_CONTROLLER)

        val deviceIds = InputDevice.getDeviceIds()

        for (i in 0 ..< deviceIds.size) {
            val device = InputDevice.getDevice(deviceIds[i])
            if (device == null || availableDevicesMap.contains(device.id)) {
                continue
            }

            createNesInputDevice(device)?.let { availableDevicesMap.put(it.id!!, it) }
        }

        // Updating the devices if they become connected/disconnected while the app is in the
        // background
        if (controller1Device?.id != null && !availableDevicesMap.contains(controller1Device?.id)) {
            controller1Device = controller1Device?.copy(id = null)
        } else {
            val device = availableDevicesMap.values
                .filter { it.descriptor == controller1Device?.descriptor }
                .firstOrNull()
            if (device != null) {
                controller1Device = device
            }
        }

        if (controller2Device?.id != null && !availableDevicesMap.contains(controller2Device?.id)) {
            controller2Device = controller2Device?.copy(id = null)
        } else {
            val device = availableDevicesMap.values
                .filter { it.descriptor == controller2Device?.descriptor }
                .firstOrNull()
            if (device != null) {
                controller2Device = device
            }
        }

        updateState()
        saveState()
    }

    fun setInputDevice(controllerId: Int, device: NesInputDevice?) {
        if (device == null) {
            if (controllerId == CONTROLLER_1) {
                controller1Device = null
            } else {
                controller2Device = null
            }
        } else if (!availableDevicesMap.contains(device.id)) {
            return
        } else {
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

        updateState()
        saveState()
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
        val button = BUTTON_MAP[event.keyCode]
        val state = BUTTON_STATE_MAP[event.action]
        if (button == null || state == null) {
            return checkPauseButtonPressed(event)
        }
        onInputEvent(event.deviceId, button, state)
        return true
    }

    private fun checkPauseButtonPressed(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP
            && (event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE
                    || event.keyCode == KeyEvent.KEYCODE_ESCAPE)) {
            emitEvent(Event.OnPauseButtonPressed)
            return true
        }
        return false
    }

    private fun loadPreferences() = coroutineScope.launch {
        controller1Device = prefManager.getController1Device()
        controller2Device = prefManager.getController2Device()

        controller1Device?.let { controllerDevice ->
            val device = availableDevicesMap.values
                .filter { it.descriptor == controllerDevice.descriptor }
                .firstOrNull()
            if (device != null) {
                controller1Device = device
            }
        }
        controller2Device?.let { controllerDevice ->
            val device = availableDevicesMap.values
                .filter { it.descriptor == controllerDevice.descriptor }
                .firstOrNull()
            if (device != null) {
                controller2Device = device
            }
        }

        if (controller1Device == null && controller2Device == null) {
            controller1Device = VIRTUAL_CONTROLLER
        }

        updateState()
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

    private fun updateState() {
        _state.update {
            it.copy(
                availableDevices = availableDevicesMap.values.toList(),
                controller1Device = controller1Device,
                controller2Device = controller2Device
            )
        }
    }

    private fun saveState() = coroutineScope.launch {
        prefManager.updateControllerDevices(controller1Device, controller2Device)
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

    private fun emitEvent(event: Event) =
        CoroutineScope(Dispatchers.IO).launch { _events.emit(event) }

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

        private val BUTTON_MAP = mapOf<Int, NesButton>(
            // Keyboard
            KeyEvent.KEYCODE_D to NesButton.DPAD_RIGHT,
            KeyEvent.KEYCODE_A to NesButton.DPAD_LEFT,
            KeyEvent.KEYCODE_S to NesButton.DPAD_DOWN,
            KeyEvent.KEYCODE_W to NesButton.DPAD_UP,
            KeyEvent.KEYCODE_I to NesButton.START,
            KeyEvent.KEYCODE_U to NesButton.SELECT,
            KeyEvent.KEYCODE_K to NesButton.A,
            KeyEvent.KEYCODE_J to NesButton.B,

            // Controller
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