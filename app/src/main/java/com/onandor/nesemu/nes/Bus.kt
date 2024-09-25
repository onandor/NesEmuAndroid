package com.onandor.nesemu.nes

class Bus {

    private companion object {
        const val MEMORY_SIZE = 2048
    }

    internal var memory: IntArray = IntArray(MEMORY_SIZE)
        private set
    internal val cpu: Cpu = Cpu(this)
    internal var cartridge: Cartridge? = null
        private set

    // Used only for the 6502 tests
    fun setMemorySize(size: Int) {
        memory = IntArray(size)
    }

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