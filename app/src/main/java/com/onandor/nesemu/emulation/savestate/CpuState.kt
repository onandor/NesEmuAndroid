package com.onandor.nesemu.emulation.savestate

@Suppress("PropertyName")
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
    val interruptCycles: Int,
    val stallCycles: Int
) : SaveState()