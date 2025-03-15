package com.onandor.nesemu.emulation.savestate

@Suppress("PropertyName")
data class CpuState(
    val PC: Int,
    val SP: Int,
    val A: Int,
    val X: Int,
    val Y: Int,
    val PS: Int
) : SaveState()