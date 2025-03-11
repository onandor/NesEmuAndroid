package com.onandor.nesemu.input

enum class NesInputDeviceType {
    CONTROLLER,
    VIRTUAL_CONTROLLER,
    KEYBOARD
}

data class NesInputDevice(
    val name: String,
    val id: Int?,
    val descriptor: String,
    val type: NesInputDeviceType
)
