package com.onandor.nesemu.input

import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.onandor.nesemu.di.IODispatcher
import com.onandor.nesemu.preferences.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
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

    data class ButtonMapKey(val playerId: Int, val inputDeviceType: NesInputDeviceType)

    data class State(
        val availableDevices: List<NesInputDevice> = emptyList(),
        val controller1Device: NesInputDevice? = null,
        val controller2Device: NesInputDevice? = null,
        val buttonMappings: Map<ButtonMapKey, BiMap<Int, NesButton>> = emptyMap()
    )

    sealed class Event {
        object OnPauseButtonPressed : Event()
    }

    private val availableDevicesMap = mutableMapOf<Int, NesInputDevice>()

    private val buttonMappings: MutableMap<ButtonMapKey, BiMap<Int, NesButton>> = mutableMapOf(
        ButtonMapKey(PLAYER_1, NesInputDeviceType.CONTROLLER)
                to HashBiMap.create(ButtonMapping.DEFAULT_CONTROLLER_BUTTON_MAP),
        ButtonMapKey(PLAYER_1, NesInputDeviceType.KEYBOARD)
                to HashBiMap.create(ButtonMapping.DEFAULT_KEYBOARD_BUTTON_MAP),
        ButtonMapKey(PLAYER_2, NesInputDeviceType.CONTROLLER)
                to HashBiMap.create(ButtonMapping.DEFAULT_CONTROLLER_BUTTON_MAP),
        ButtonMapKey(PLAYER_2, NesInputDeviceType.KEYBOARD)
                to HashBiMap.create(ButtonMapping.DEFAULT_KEYBOARD_BUTTON_MAP)
    )

    private val player1Buttons: MutableMap<NesButton, NesButtonState> = initControllerButtons()
    private val player2Buttons: MutableMap<NesButton, NesButtonState> = initControllerButtons()
    var player1Device: NesInputDevice? = null
        private set
    var player2Device: NesInputDevice? = null
        private set

    // TODO: set this to false whenever menus need to be controlled
    private var gameRunning: Boolean = true

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    private val inputDeviceListener = object : InputManager.InputDeviceListener {

        override fun onInputDeviceAdded(deviceId: Int) {
            val device = InputDevice.getDevice(deviceId)
            if (device == null || availableDevicesMap.contains(deviceId)) {
                return
            }
            createNesInputDevice(device)?.let {
                availableDevicesMap.put(it.id!!, it)
                if (player1Device?.descriptor == it.descriptor) {
                    player1Device = it
                } else if (player2Device?.descriptor == it.descriptor) {
                    player2Device = it
                }
                updateState()
            }
            Log.d(TAG, "Input device added: ${device.name} (id: $deviceId)")
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            val nesDevice = availableDevicesMap.remove(deviceId)
            nesDevice?.let {
                if (player1Device == it) {
                    player1Device = player1Device?.copy(id = null)
                } else if (player2Device == it) {
                    player2Device = player2Device?.copy(id = null)
                }
                updateState()
                persistInputDevices()
            }
            Log.d(TAG, "Input device removed: ${nesDevice?.name} (id: ${nesDevice?.id})")
        }

        override fun onInputDeviceChanged(deviceId: Int) {}
    }

    init {
        refreshAvailableDevices()
        loadSavedInputDevices()
        loadSavedButtonMappings()
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
        if (player1Device?.id != null && !availableDevicesMap.contains(player1Device?.id)) {
            player1Device = player1Device?.copy(id = null)
        } else {
            val device = availableDevicesMap.values
                .filter { it.descriptor == player1Device?.descriptor }
                .firstOrNull()
            if (device != null) {
                player1Device = device
            }
        }

        if (player2Device?.id != null && !availableDevicesMap.contains(player2Device?.id)) {
            player2Device = player2Device?.copy(id = null)
        } else {
            val device = availableDevicesMap.values
                .filter { it.descriptor == player2Device?.descriptor }
                .firstOrNull()
            if (device != null) {
                player2Device = device
            }
        }

        updateState()
        persistInputDevices()
    }

    fun setInputDevice(playerId: Int, device: NesInputDevice?) {
        if (device == null) {
            if (playerId == PLAYER_1) {
                player1Device = null
            } else {
                player2Device = null
            }
        } else if (!availableDevicesMap.contains(device.id)) {
            return
        } else {
            if (playerId == PLAYER_1) {
                player1Device = device
                if (player2Device == device) {
                    player2Device = null
                }
            } else {
                player2Device = device
                if (player1Device == device) {
                    player1Device = null
                }
            }
        }

        updateState()
        persistInputDevices()
    }

    fun onInputEvents(deviceId: Int, buttonStates: Map<NesButton, NesButtonState>) {
        if (player1Device?.id == deviceId) {
            player1Buttons.putAll(buttonStates)
        } else if (player2Device?.id == deviceId) {
            player2Buttons.putAll(buttonStates)
        }
    }

    fun onInputEvent(deviceId: Int, button: NesButton, state: NesButtonState) {
        if (player1Device?.id == deviceId) {
            player1Buttons[button] = state
        } else if (player2Device?.id == deviceId) {
            player2Buttons[button] = state
        }
    }

    fun onInputEvent(event: KeyEvent): Boolean {
        val device = availableDevicesMap[event.deviceId]
        if (device == null) {
            // The event came from a device which is not a valid controller, we don't handle it
            return false
        }

        val playerId = if (player1Device?.id == device.id) {
            PLAYER_1
        } else if (player2Device?.id == device.id) {
            PLAYER_2
        } else {
            // The event came from a device which is currently not mapped to either NES controllers
            // Pause requests are handled for all controllers
            return checkPauseButtonPressed(event)
        }

        val buttonMap = buttonMappings[ButtonMapKey(playerId, device.type)]!!

        val button = buttonMap[event.keyCode]
        val state = BUTTON_STATE_MAP[event.action]
        if (button == null || state == null) {
            // The event came from a mapped device, but the actual button is not mapped
            return checkPauseButtonPressed(event)
        }

        onInputEvent(event.deviceId, button, state)
        return gameRunning
    }

    private fun checkPauseButtonPressed(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP
            && (event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE
                    || event.keyCode == KeyEvent.KEYCODE_ESCAPE)) {
            _events.tryEmit(Event.OnPauseButtonPressed)
        }
        return gameRunning
    }

    private fun loadSavedInputDevices() = coroutineScope.launch {
        player1Device = prefManager.getController1Device()
        player2Device = prefManager.getController2Device()

        player1Device?.let { playerDevice ->
            val device = availableDevicesMap.values
                .filter { it.descriptor == playerDevice.descriptor }
                .firstOrNull()
            if (device != null) {
                player1Device = device
            }
        }
        player2Device?.let { playerDevice ->
            val device = availableDevicesMap.values
                .filter { it.descriptor == playerDevice.descriptor }
                .firstOrNull()
            if (device != null) {
                player2Device = device
            }
        }

        if (player1Device == null && player2Device == null) {
            player1Device = VIRTUAL_CONTROLLER
        }

        updateState()
    }

    private fun loadSavedButtonMappings() = coroutineScope.launch {
        prefManager.getButtonMappings().forEach { key, value ->
            if (value.isNotEmpty()) {
                buttonMappings[key] = value
            }
        }
    }

    fun registerListener() {
        inputManager.registerInputDeviceListener(inputDeviceListener, null)
        refreshAvailableDevices()
    }

    fun unregisterListener() {
        inputManager.unregisterInputDeviceListener(inputDeviceListener)
        availableDevicesMap.clear()
    }

    fun getButtonStates(playerId: Int): Int {
        val buttonStateMap = if (playerId == PLAYER_1) player1Buttons else player2Buttons
        var buttonStates = 0
        buttonStateMap.forEach { _, state ->
            buttonStates = (buttonStates shl 1) or state.ordinal
        }
        return buttonStates
    }

    fun changeButtonMapping(
        keyCode: Int,
        button: NesButton,
        playerId: Int,
        deviceType: NesInputDeviceType
    ) {
        val keyCodeMap = if (deviceType == NesInputDeviceType.CONTROLLER) {
            ButtonMapping.CONTROLLER_KEYCODE_ICON_MAP
        } else {
            ButtonMapping.KEYBOARD_KEYCODE_ICON_MAP
        }

        if (!keyCodeMap.contains(keyCode)) {
            return
        }

        buttonMappings[ButtonMapKey(playerId, deviceType)]!!.forcePut(keyCode, button)

        updateState()
        persistButtonMappings()
    }

    private fun updateState() {
        _state.update {
            it.copy(
                availableDevices = availableDevicesMap.values.toList(),
                controller1Device = player1Device,
                controller2Device = player2Device,
                buttonMappings = buttonMappings
            )
        }
    }

    private fun persistInputDevices() = coroutineScope.launch {
        prefManager.updateInputDevices(player1Device, player2Device)
    }

    private fun persistButtonMappings() = coroutineScope.launch {
        prefManager.updateButtonMappings(buttonMappings)
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

        const val PLAYER_1: Int = 1
        const val PLAYER_2: Int = 2

        const val VIRTUAL_CONTROLLER_DEVICE_ID = Int.MIN_VALUE
        const val VIRTUAL_CONTROLLER_DEVICE_DESCRIPTOR = "VIRTUAL_CONTROLLER"
        private val VIRTUAL_CONTROLLER = NesInputDevice(
            name = "Virtual controller",
            id = VIRTUAL_CONTROLLER_DEVICE_ID,
            descriptor = VIRTUAL_CONTROLLER_DEVICE_DESCRIPTOR,
            type = NesInputDeviceType.VIRTUAL_CONTROLLER
        )

        private val BUTTON_STATE_MAP = mapOf<Int, NesButtonState>(
            KeyEvent.ACTION_UP to NesButtonState.UP,
            KeyEvent.ACTION_DOWN to NesButtonState.DOWN
        )
    }
}