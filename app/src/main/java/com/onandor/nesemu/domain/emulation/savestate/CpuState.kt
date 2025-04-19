package com.onandor.nesemu.domain.emulation.savestate

import com.onandor.nesemu.domain.emulation.nes.cpu.IRQSource
import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class CpuState(
    val PC: Int,
    val SP: Int,
    val A: Int,
    val X: Int,
    val Y: Int,
    val PS: Int,
    val instruction: Int,
    val eaddress: Int,
    val addressingCycle: Boolean,
    val instructionCycle: Boolean,
    val totalCycles: Int,
    val stallCycles: Int,
    val nmiSignaled: Boolean,
    val irqSignals: Set<IRQSource>
) : SaveState()