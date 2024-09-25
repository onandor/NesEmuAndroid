package com.onandor.nesemu.nes

class Nes {

    private val memory: Memory = Memory()
    private val cpu: Cpu = Cpu(memory)
    private var cartridge: Cartridge? = null

    fun insertCartridge(cartridge: Cartridge) {
        this.cartridge = cartridge
    }

    fun reset() {
        cpu.reset()
    }
}