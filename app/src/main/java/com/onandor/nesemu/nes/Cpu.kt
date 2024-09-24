package com.onandor.nesemu.nes

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

class Cpu(private val memory: Memory) {

    object Flags {
        const val CARRY: U8 = 0b00000001.toByte()
        const val ZERO: U8 = 0b00000010.toByte()
        const val INTERRUPT: U8 = 0b00000100.toByte()
        const val DECIMAL: U8 = 0b00001000.toByte()
        const val BREAK: U8 = 0b00010000.toByte()
        const val UNUSED: U8 = 0b00100000.toByte()
        const val OVERFLOW: U8 = 0b01000000.toByte()
        const val NEGATIVE: U8 = 0b10000000.toByte()
    }

    private var PC: U16 = 0xFFFCu               // Program Counter - 16 bits
    private var SP: U8 = 0xFD.toByte()          // Stack Pointer - 8 bits
    private var A: U8 = 0.toByte()              // Accumulator - 8 bits
    private var X: U8 = 0.toByte()              // Index Register X - 8 bits
    private var Y: U8 = 0.toByte()              // Index Register Y - 8 bits
    private var PS: U8 = 0b00010100.toByte()    // Processor Status - 8 bits (flags)

    private var opcode: U8 = 0                  // Currently executed opcode
    private var eaddress: U16 = 0U              // Effective address for the current instruction

    private var addressingCycle = false
    private var opcodeCycle = false

    private var cycles: U32 = 0U

    init {
        reset()
    }

    fun reset() {
        PC = 0xFFFCu
        SP = 0x00FD.toByte()
        A = 0.toByte()
        X = 0.toByte()
        Y = 0.toByte()
        cycles = 0U
    }

    fun execute(cycles: U32): U32 {
        this.cycles = 0U
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

    private fun readByte(address: U16): U8 = memory[address]

    private fun read2Bytes(address: U16): U8 {
        return memory[address] or (memory[address].shl(8))
    }

    private fun writeByte(address: U16, value: U8) {
        memory[address] = value
    }

    private fun setFlag(flag: U8, set: Boolean) {
        PS = if (set) PS or flag else PS and flag.inv()
    }

    private fun getFlag(flag: U8): Boolean = (PS and flag) > 0
}