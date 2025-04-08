package com.onandor.nesemu.domain.emulation.savestate

import com.onandor.nesemu.domain.emulation.nes.Mirroring
import kotlinx.serialization.Serializable

@Serializable
data class CartridgeState(
    val prgRom: IntArray,
    val chrRom: IntArray?,
    val prgRam: IntArray?,
    val chrRam: IntArray?,
    val mirroring: Mirroring
) : SaveState()