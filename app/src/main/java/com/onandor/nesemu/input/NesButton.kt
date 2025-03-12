package com.onandor.nesemu.input

enum class NesButtonState {
    UP, DOWN
}

// The order _cannot_ change because ordinals are used in the protobuf schema during serialization
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
