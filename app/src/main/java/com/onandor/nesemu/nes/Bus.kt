package com.onandor.nesemu.nes

class Bus {

    private companion object {
        const val MEMORY_SIZE = 2048
    }

    private var memory: IntArray = IntArray(MEMORY_SIZE)
    private val cpu: Cpu = Cpu(this::readMemory, this::writeMemory)
    private var cartridge: Cartridge? = null

    fun readMemory(address: Int): Int {
        return memory[address]
    }

    fun writeMemory(address: Int, value: Int) {
        memory[address] = value
    }

    fun insertCartridge(cartridge: Cartridge) {
        this.cartridge = cartridge
    }

    fun reset() {
        cpu.reset()
    }
}