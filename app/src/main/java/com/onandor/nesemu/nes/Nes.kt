package com.onandor.nesemu.nes

import android.util.Log
import com.onandor.nesemu.nes.mappers.Mapper
import com.onandor.nesemu.nes.mappers.Mapper0
import com.onandor.nesemu.nes.mappers.Mapper3
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

class Nes {

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
    private var lastValueRead: Int = 0
    private var buttonStates: Int = 0

    var running: Boolean = true
    private var isFrameReady: Boolean = false
    private var numFrames: Int = 0
    var fps: Float = 0f
        private set

    private val listeners = mutableListOf<NesListener>()

    fun cpuReadMemory(address: Int): Int {
        lastValueRead = when (address) {
            in 0x0000 .. 0x1FFF -> cpuMemory[address and 0x07FF]        // 2 KB RAM with mirroring
            in 0x2000 .. 0x3FFF -> ppu.cpuReadRegister(address and 0x2007) // I/O Registers with mirroring
            in 0x4000 .. 0x4015 -> 0                                    // APU registers
            0x4016, 0x4017 -> {                                               // Controller
                val state = buttonStates and 0x01
                buttonStates = buttonStates ushr 1
                state
            }
            0x4018, 0x4019 -> lastValueRead                                   // Unused? (open bus set for now)
            in 0x4020 .. 0x5FFF -> {                                    // Usually unmapped
                val value = mapper.readUnmappedRange(address)
                if (value == -1) lastValueRead else value
            }
            in 0x6000 .. 0x7FFF -> {                                    // Usually cartridge SRAM
                val value = mapper.readRam(address)
                if (value == -1) lastValueRead else value
            }
            in 0x8000 .. 0xFFFF -> mapper.readPrgRom(address)           // PRG-ROM
            else -> throw InvalidOperationException(TAG,
                "Invalid CPU read at $${address.toHexString(4)}")
        }
        return lastValueRead
    }

    fun cpuWriteMemory(address: Int, value: Int) {
        when (address) {
            in 0x0000 .. 0x1FFF -> cpuMemory[address and 0x07FF] = value        // 2 KB RAM with mirroring
            in 0x2000 .. 0x3FFF -> ppu.cpuWriteRegister(address and 0x2007, value) // I/O Registers
            0x4014 -> {
                val oamData = cpuMemory.copyOfRange(value shl 8, ((value shl 8) or 0x00FF) + 1)
                ppu.loadOamData(oamData)
            }
            in 0x4000 .. 0x4015, 0x4017 -> 0                                    // APU registers
            0x4016 -> pollButtonStates()                                              // Controller
            0x4018, 0x4019 -> lastValueRead                                           // Unused? (open bus set for now)
            in 0x4020 .. 0x5FFF -> mapper.writeUnmappedRange(address, value)    // Usually unmapped
            in 0x6000 .. 0x7FFF -> mapper.writeRam(address, value)              // Usually cartridge SRAM
            in 0x8000 .. 0xFFFF -> mapper.writePrgRom(address, value)           // PRG-ROM
            else -> throw InvalidOperationException(TAG,
                "Invalid CPU write at $${address.toHexString(4)}")
        }
    }

    fun ppuReadMemory(address: Int): Int {
        val mirroredAddress = address and 0x3FFF
        return when (mirroredAddress) {
            in 0x0000 .. 0x1FFF -> mapper.readChrRom(address) // Pattern Table
            in 0x2000 .. 0x2FFF -> vram[mapper.mapNametableAddress(address)]    // Nametables
            in 0x3000 .. 0x3EFF -> ppuReadMemory(address and 0x2EFF) // Mirror of 0x2000-0x2EFF
            in 0x3F00 .. 0x3F1F -> 0    // Palette (access handled by PPU internally)
            in 0x3F20 .. 0x3FFF -> 0    // Mirror of 0x3F00-0x3F1F
            else -> throw InvalidOperationException(TAG,
                "Invalid PPU read at $${address.toHexString(4)}")
        }
    }

    fun ppuWriteMemory(address: Int, value: Int) {
        val mirroredAddress = address and 0x3FFF
        when (mirroredAddress) {
            in 0x0000 .. 0x1FFF -> mapper.writeChrRom(address, value) // Pattern Table
            in 0x2000 .. 0x2FFF -> vram[mapper.mapNametableAddress(address)] = value    // Nametables
            in 0x3000 .. 0x3EFF -> ppuWriteMemory(address and 0x2EFF, value) // Mirror of 0x2000-0x2EFF
            in 0x3F00 .. 0x3F1F -> {}    // Palette (access handled by PPU internally)
            in 0x3F20 .. 0x3FFF -> {}    // Mirror of 0x3F00-0x3F1F
            else -> throw InvalidOperationException(TAG,
                "Invalid PPU write at $${address.toHexString(4)}")
        }
    }

    fun insertCartridge(cartridge: Cartridge) {
        this.cartridge = cartridge
        mapper = when (cartridge.mapperId) {
            0 -> Mapper0(cartridge)
            3 -> Mapper3(cartridge)
            else -> throw RomParseException(TAG, "Unsupported mapper: ${cartridge.mapperId}")
        }
        ppu.mirroring = cartridge.mirroring
    }

    private fun ppuFrameReady() {
        isFrameReady = true
        listeners.forEach { it.onFrameReady() }
    }

    suspend fun reset() {
        cpuMemory = IntArray(MEMORY_SIZE)
        vram = IntArray(MEMORY_SIZE)
        lastValueRead = 0
        numFrames = 0
        isFrameReady = false
        running = true
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

    fun pollButtonStates() {
        listeners.forEach { it.onReadButtons() }
    }

    fun setButtonStates(buttonStates: Int) {
        this.buttonStates = buttonStates
    }

    fun registerListener(listener: NesListener) {
        this.listeners.add(listener)
    }

    fun unregisterListener(listener: NesListener) {
        this.listeners.remove(listener)
    }

    // Functions used for debugging

    fun dbgCpuReadMemory(address: Int): Int {
        return when (address) {
            in 0x0000 .. 0x1FFF -> cpuMemory[address and 0x07FF]        // 2 KB RAM with mirroring
            in 0x2000 .. 0x3FFF -> ppu.dbgCpuReadRegister(address and 0x2007) // I/O Registers with mirroring
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

    fun setDebugFeatureBool(feature: DebugFeature, value: Boolean) {
        when (feature) {
            DebugFeature.PPU_RENDER_PATTERN_TABLE -> {
                ppu.dbgDrawPatternTable = value
            }
            DebugFeature.PPU_RENDER_NAMETABLE -> {
                ppu.dbgDrawNametable = value
            }
            DebugFeature.PPU_RENDER_COLOR_PALETTES -> {
                ppu.dbgDrawColorPalettes = value
            }
            else -> {}
        }
    }

    fun setDebugFeatureInt(feature: DebugFeature, value: Int) {
        when (feature) {
            DebugFeature.PPU_SET_COLOR_PALETTE -> {
                ppu.dbgColorPaletteId = value
            }
            else -> {}
        }
    }
}