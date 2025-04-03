package com.onandor.nesemu.domain.input

enum class NesButtonState {
    Up, Down
}

// The order _cannot_ change because ordinals are used in the protobuf schema during serialization
enum class NesButton {
    DPadRight,
    DPadLeft,
    DPadDown,
    DPadUp,
    Start,
    Select,
    B,
    A
}
