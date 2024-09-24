package com.onandor.nesemu.nes

class Cpu(private val memory: Memory) {

    private object Flags {
        const val CARRY: Int = 0b00000001
        const val ZERO: Int = 0b00000010
        const val INTERRUPT: Int = 0b00000100
        const val DECIMAL: Int = 0b00001000
        const val BREAK: Int = 0b00010000
        const val UNUSED: Int = 0b00100000
        const val OVERFLOW: Int = 0b01000000
        const val NEGATIVE: Int = 0b10000000
    }

    private var PC: Int = 0xFFFC            // Program Counter - 16 bits
    private var SP: Int = 0xFD              // Stack Pointer - 8 bits
    private var A: Int = 0                  // Accumulator - 8 bits
    private var X: Int = 0                  // Index Register X - 8 bits
    private var Y: Int = 0                  // Index Register Y - 8 bits
    private var PS: Int = 0b00010100        // Processor Status - 8 bits (flags)

    private var opcode: Int = 0             // Currently executed opcode
    private var eaddress: Int = 0           // Effective address for the current instruction

    private var addressingCycle = false
    private var opcodeCycle = false

    private var cycles: Int = 0

    init {
        reset()
    }

    fun reset() {
        SP = 0x00FD
        A = 0
        X = 0
        Y = 0
        cycles = 0
    }

    fun execute(cycles: Int): Int {
        this.cycles = 0
        while (this.cycles < cycles) {
            addressingCycle = false
            opcodeCycle = false

            opcode = readByte(PC++)
            // get effective address
            // get opcode
            // increment cycles by the correct amount
            if (addressingCycle && opcodeCycle) {
                this.cycles++
            }
        }
        return this.cycles
    }

    private fun readByte(address: Int): Int = memory[address and 0xFFFF]

    private fun read2Bytes(address: Int): Int =
        memory[address and 0xFFFF] or (memory[(address + 1) and 0xFFFF] shl 8)

    private fun writeByte(address: Int, value: Int) {
        memory[address and 0xFFFF] = value
    }

    private fun setFlag(flag: Int, set: Boolean) {
        PS = if (set) PS or flag else PS and flag.inv()
    }

    private fun getFlag(flag: Int): Boolean = (PS and flag) > 0

    private fun incrPC(incrementBy: Int): Int {
        val oldPC = PC
        PC = (PC + incrementBy) and 0xFFFF
        return oldPC
    }

    // ------ Addressing mode functions ------

    // Accumulator
    private fun acc() {}

    // Absolute
    private fun abs() {
        eaddress = readByte(incrPC(1)) or (readByte(incrPC(1)) shl 8)
    }

    // Absolute,X
    private fun absx() {
        val address = readByte(PC)
        eaddress = address + X
        addressingCycle = (address xor eaddress) shr 8 > 0
        incrPC(2)
    }

    // Absolute,Y
    private fun absy() {
        val address = read2Bytes(PC);
        eaddress = address + Y
        addressingCycle = (address xor eaddress) shr 8 > 0
        incrPC(2)
    }

    // Immediate
    private fun imm() {
        eaddress = incrPC(1)
    }

    // Implied
    private fun impl() {}

    // Indirect
    private fun ind() {
        val address = read2Bytes(PC)
        incrPC(2)
        eaddress = if ((address and 0xFF) == 0xFF) {
            readByte(address) or (readByte(address and 0xFF00) shl 8)
        } else {
            read2Bytes(address)
        }
    }

    // Indexed Indirect
    private fun indx() {
        val pageZeroAddress = readByte(incrPC(1)) + X
        val nextPageZeroAddress = (pageZeroAddress + 1) and 0xFF // Zero page wrap-around
        eaddress = readByte(pageZeroAddress) or (readByte(nextPageZeroAddress) shl 8)
    }

    // Indirect Indexed
    private fun indy() {
        val pageZeroAddress = readByte(PC++)
        val nextPageZeroAddress = (pageZeroAddress + 1) and 0xFF // Zero page wrap around
        val address = readByte(pageZeroAddress) or (readByte(nextPageZeroAddress) shl 8)
        val addressY = address + Y
        addressingCycle = (address xor addressY) shr 8 > 0
        eaddress = addressY
    }

    // Relative
    private fun rel() {
        eaddress = PC++
    }

    // Zero Page
    private fun zpg() {
        eaddress = readByte(PC++)
    }

    // Zero Page,X
    private fun zpgx() {
        eaddress = (readByte(PC++) + X) and 0xFF
    }

    // Zero Page,Y
    private fun zpgy() {
        eaddress = (readByte(PC++) + Y) and 0xFF
    }
}































