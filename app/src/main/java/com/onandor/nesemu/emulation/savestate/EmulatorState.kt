package com.onandor.nesemu.emulation.savestate

data class EmulatorState(
    val cpuMemory: IntArray,
    val lastValueRead: Int,
    val vram: IntArray,
    val cpu: CpuState,
    val ppu: PpuState,
    val apu: ApuState,
    val cartridge: CartridgeState
) : SaveState()
