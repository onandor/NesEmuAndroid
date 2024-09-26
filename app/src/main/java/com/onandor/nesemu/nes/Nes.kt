package com.onandor.nesemu.nes

import com.onandor.nesemu.nes.mappers.Mapper
import com.onandor.nesemu.nes.mappers.Mapper0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Nes {

    private companion object {
        const val TAG = "Nes"
        const val CPU_MEMORY_SIZE = 2048
    }

    private var cpuMemory: IntArray = IntArray(CPU_MEMORY_SIZE)
    private val cpu: Cpu = Cpu(::cpuReadMemory, ::cpuWriteMemory)
    private var cartridge: Cartridge? = null
    private lateinit var mapper: Mapper

    fun cpuReadMemory(address: Int): Int {
        return when (address) {
            in 0x0000 .. 0x07FF -> cpuMemory[address]              // 2 KB RAM
            in 0x0800 .. 0x17FF -> cpuMemory[address and 0x07FF]   // 2x Mirror of RAM
            in 0x2000 .. 0x2007 -> 0                               // I/O Registers
            in 0x2008 .. 0x3FFF -> 0                               // Mirror of I/O Registers
            in 0x4000 .. 0x4019 -> 0                               // Registers (Mostly APU)
            in 0x4020 .. 0x5FFF -> {                               // Cartridge Expansion ROM
                throw InvalidOperationException(TAG, "CPU read at $address: Cartridge Expansion ROM not supported")
            }
            in 0x6000 .. 0x7FFF -> 0                               // SRAM
            in 0x8000 .. 0xFFFF -> mapper.readPrgRom(address)      // PRG-ROM
            else -> {
                throw InvalidOperationException(TAG, "Invalid CPU read at $address")
            }
        }
    }

    fun cpuWriteMemory(address: Int, value: Int) {
        when (address) {
            in 0x0000 .. 0x07FF -> cpuMemory[address] = value               // 2 KB RAM
            in 0x0800 .. 0x17FF -> cpuMemory[address and 0x07FF] = value    // 2x Mirror of RAM
            in 0x2000 .. 0x2007 -> 0                                        // I/O Registers
            in 0x2008 .. 0x3FFF -> 0                                        // Mirror of I/O Registers
            in 0x4000 .. 0x4019 -> 0                                        // Registers (Mostly APU)
            in 0x4020 .. 0x5FFF -> {                                        // Cartridge Expansion ROM
                val address = address.toString(16)
                throw InvalidOperationException(TAG, "CPU write at $$address: Cartridge Expansion ROM not supported")
            }
            in 0x6000 .. 0x7FFF -> 0                                        // SRAM
            in 0x8000 .. 0xFFFF -> mapper.writePrgRom(address, value)       // PRG-ROM
            else -> {
                throw InvalidOperationException(TAG, "Invalid CPU write at $address")
            }
        }
    }

    fun insertCartridge(cartridge: Cartridge) {
        this.cartridge = cartridge
        when (cartridge.mapperId) {
            0 -> mapper = Mapper0(cartridge)
            else -> return
        }
    }

    fun reset() {
        cpu.reset()
    }
}