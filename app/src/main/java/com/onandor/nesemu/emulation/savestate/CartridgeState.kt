package com.onandor.nesemu.emulation.savestate

data class CartridgeState(
    val filePath: String,
    val prgRom: IntArray,
    val chrRom: IntArray,
    val prgRam: IntArray?,
    val chrRam: IntArray?
) : SaveState()