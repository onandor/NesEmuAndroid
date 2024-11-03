package com.onandor.nesemu.nes

import android.util.Log
import com.onandor.nesemu.nes.mappers.Mapper
import com.onandor.nesemu.nes.mappers.Mapper0
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

class Nes(val frameReady: (IntArray) -> Unit) {

    private companion object {
        const val TAG = "Nes"
        const val MEMORY_SIZE = 2048
        private const val FPS = 60
    }

    private var cpuMemory: IntArray = IntArray(MEMORY_SIZE)
    private var vram: IntArray = IntArray(MEMORY_SIZE)
    val cpu: Cpu = Cpu(::cpuReadMemory, ::cpuWriteMemory)
    val ppu: Ppu = Ppu(::ppuReadMemory, ::ppuWriteMemory, cpu::NMI, ::ppuFrameReady)
    private var cartridge: Cartridge? = null
    private lateinit var mapper: Mapper

    var running: Boolean = true
    private var isFrameReady: Boolean = false
    private var numFrames: Int = 0
    var fps: Float = 0f
        private set

    fun cpuReadMemory(address: Int): Int {
        return when (address) {
            in 0x0000 .. 0x1FFF -> cpuMemory[address and 0x07FF]        // 2 KB RAM with mirroring
            in 0x2000 .. 0x3FFF -> ppu.cpuReadRegister(address and 0x2007) // I/O Registers with mirroring
            in 0x4000 .. 0x4019 -> 0                                    // Registers (Mostly APU)
            in 0x4020 .. 0x5FFF -> {                                    // Cartridge Expansion ROM
                throw InvalidOperationException(TAG,
                    "CPU read at $address: Cartridge Expansion ROM not supported")
            }
            in 0x6000 .. 0x7FFF -> 0                                    // SRAM
            in 0x8000 .. 0xFFFF -> mapper.readPrgRom(address)           // PRG-ROM
            else -> throw InvalidOperationException(TAG, "Invalid CPU read at $address")
        }
    }

    fun cpuWriteMemory(address: Int, value: Int) {
        when (address) {
            in 0x0000 .. 0x1FFF -> cpuMemory[address and 0x07FF] = value        // 2 KB RAM with mirroring
            in 0x2000 .. 0x3FFF -> ppu.cpuWriteRegister(address and 0x2007, value) // I/O Registers
            0x4014 -> {
                val oamData = cpuMemory
                    .copyOfRange(value shl 8, (value shl 8) or 0x00FF)
                ppu.loadOamData(oamData)
                // TODO?: CPU suspend - https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_DMA_($4014)_%3E_write
            }
            in 0x4000 .. 0x4019 -> 0                                            // Registers (Mostly APU)
            in 0x4020 .. 0x5FFF -> {                                            // Cartridge Expansion ROM
                throw InvalidOperationException(TAG,
                    "CPU write at $address: Cartridge Expansion ROM not supported")
            }
            in 0x6000 .. 0x7FFF -> 0                                            // SRAM
            in 0x8000 .. 0xFFFF -> mapper.writePrgRom(address, value)           // PRG-ROM
            else -> throw InvalidOperationException(TAG, "Invalid CPU write at $address")
        }
    }

    fun ppuReadMemory(address: Int): Int {
        val mirroredAddress = address and 0x3FFF
        return when (mirroredAddress) {
            in 0x0000 .. 0x1FFF -> mapper.readChrRom(address) // Pattern Table
            in 0x2000 .. 0x2FFF -> vram[mapper.mapNametableAddress(address)]    // Nametables
            in 0x3000 .. 0x3EFF -> ppuReadMemory(address and 0x2EFF) // Mirror of 0x2000-0x2EFF
            in 0x3F00 .. 0x3F0F -> 0    // Background Palette
            in 0x3F10 .. 0x3F1F -> 0    // Sprite Palette
            in 0x3F20 .. 0x3FFF -> ppuReadMemory(address and 0x3F1F) // Mirror of 0x3F00-0x3F1F
            else -> throw InvalidOperationException(TAG, "Invalid PPU read at $address")
        }
    }

    fun ppuWriteMemory(address: Int, value: Int) {
        val mirroredAddress = address and 0x3FFF
        when (mirroredAddress) {
            in 0x0000 .. 0x1FFF -> mapper.writeChrRom(address, value) // Pattern Table
            in 0x2000 .. 0x2FFF -> vram[mapper.mapNametableAddress(address)] = value    // Nametables
            in 0x3000 .. 0x3EFF -> ppuWriteMemory(address and 0x2EFF, value) // Mirror of 0x2000-0x2EFF
            in 0x3F00 .. 0x3F0F -> 0    // Background Palette
            in 0x3F10 .. 0x3F1F -> 0    // Sprite Palette
            in 0x3F20 .. 0x3FFF -> ppuWriteMemory(address and 0x3F1F, value) // Mirror of 0x3F00-0x3F1F
            else -> throw InvalidOperationException(TAG, "Invalid PPU write at $address")
        }
    }

    fun insertCartridge(cartridge: Cartridge) {
        this.cartridge = cartridge
        when (cartridge.mapperId) {
            0 -> mapper = Mapper0(cartridge)
            else -> return
        }
        ppu.mirroring = cartridge.mirroring
    }

    private fun ppuFrameReady(frameData: IntArray) {
        isFrameReady = true
        frameReady(frameData)
    }

    suspend fun reset() {
        cpuMemory = IntArray(MEMORY_SIZE)
        vram = IntArray(MEMORY_SIZE)
        numFrames = 0
        isFrameReady = false
        cpu.reset()
        ppu.reset()

        val timeSource = TimeSource.Monotonic
        var fpsMeasureStart = timeSource.markNow()
        while (running) {
            val frameStart = timeSource.markNow()
            while (!isFrameReady) {
                val cpuCycles = cpu.step()
                for (i in 0 until cpuCycles * 3) {
                    ppu.tick()
                }
            }
            isFrameReady = false
            numFrames++

            val now = timeSource.markNow()
            // If we are lagging behind, the delta will be negative and the call to delay will
            // return immediately
            delay(1000 / FPS - (now - frameStart).inWholeMilliseconds)

            if ((now - fpsMeasureStart).inWholeMilliseconds >= 3000) {
                fps = numFrames / 3f
                numFrames = 0
                fpsMeasureStart = timeSource.markNow()
                Log.i(TAG, "FPS: $fps")
            }
        }
    }

    // Functions used for debugging

    fun cpuReadMemory_dbg(address: Int): Int {
        return when (address) {
            in 0x0000 .. 0x1FFF -> cpuMemory[address and 0x07FF]        // 2 KB RAM with mirroring
            in 0x2000 .. 0x3FFF -> ppu.cpuReadRegister_dbg(address and 0x2007) // I/O Registers with mirroring
            in 0x4000 .. 0x4019 -> 0                                    // Registers (Mostly APU)
            in 0x4020 .. 0x5FFF -> {                                    // Cartridge Expansion ROM
                throw InvalidOperationException(TAG,
                    "CPU read at $address: Cartridge Expansion ROM not supported")
            }
            in 0x6000 .. 0x7FFF -> 0                                    // SRAM
            in 0x8000 .. 0xFFFF -> mapper.readPrgRom(address)           // PRG-ROM
            else -> throw InvalidOperationException(TAG, "Invalid CPU read at $address")
        }
    }
}