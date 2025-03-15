package com.onandor.nesemu.emulation.savestate

data class NesState(
    val cpuMemory: IntArray,
    val lastValueRead: Int,
    val vram: IntArray,
    val cpu: CpuState,
    val ppu: PpuState,
    val apu: ApuState,
    val cartridge: CartridgeState,
    val mapper: MapperState
) : SaveState()
