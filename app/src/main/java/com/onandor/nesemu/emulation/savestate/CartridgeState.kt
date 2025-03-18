package com.onandor.nesemu.emulation.savestate

import kotlinx.serialization.Serializable

@Serializable
data class CartridgeState(
    val prgRom: IntArray,
    val chrRom: IntArray?,
    val prgRam: IntArray?,
    val chrRam: IntArray?
) : SaveState()