package com.onandor.nesemu.input

enum class NesButtonState {
    UP, DOWN
}

enum class NesButton {
    DPAD_RIGHT,
    DPAD_LEFT,
    DPAD_DOWN,
    DPAD_UP,
    START,
    SELECT,
    B,
    A
}

class InputHandler {

    var device: NesInputDevice? = null
    val buttonStateMap = mutableMapOf<NesButton, NesButtonState>(
        NesButton.DPAD_RIGHT to NesButtonState.UP,
        NesButton.DPAD_LEFT to NesButtonState.UP,
        NesButton.DPAD_DOWN to NesButtonState.UP,
        NesButton.DPAD_UP to NesButtonState.UP,
        NesButton.START to NesButtonState.UP,
        NesButton.SELECT to NesButtonState.UP,
        NesButton.B to NesButtonState.UP,
        NesButton.A to NesButtonState.UP
    )

    fun onKeyEvent(button: NesButton, state: NesButtonState) {
        if (device == null) {
            return
        }
        buttonStateMap[button] = state
    }

    fun getNesButtonStates(): Int {
        if (device == null) {
            return 0
        }

        var buttonStates = 0
        buttonStateMap.forEach { _, state ->
            buttonStates = (buttonStates shl 1) or state.ordinal
        }
        return buttonStates
    }
}