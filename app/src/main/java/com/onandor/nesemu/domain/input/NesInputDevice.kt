package com.onandor.nesemu.domain.input

enum class NesInputDeviceType {
    Controller,
    VirtualController,
    Keyboard
}

data class NesInputDevice(
    val name: String,
    val id: Int?,
    val descriptor: String,
    val type: NesInputDeviceType
)
