package com.onandor.nesemu.emulation.nes

import android.util.Log
import com.onandor.nesemu.emulation.nes.mappers.Mapper
import com.onandor.nesemu.emulation.nes.mappers.Mapper0
import com.onandor.nesemu.emulation.nes.mappers.Mapper2
import com.onandor.nesemu.emulation.nes.mappers.Mapper3
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

class Nes {

    private companion object {
        const val TAG = "Nes"
        const val MEMORY_SIZE = 2048
        const val TARGET_FPS = 60
    }

    private var cpuMemory: IntArray = IntArray(MEMORY_SIZE)
    private var vram: IntArray = IntArray(MEMORY_SIZE)
    val cpu: Cpu = Cpu(::cpuReadMemory, ::cpuWriteMemory)
    val ppu: Ppu = Ppu(::ppuReadMemory, ::ppuWriteMemory, cpu::NMI, ::ppuFrameReady)
    val apu: Apu = Apu(cpu::IRQ)
    private var cartridge: Cartridge? = null
    private lateinit var mapper: Mapper

    private var lastValueRead: Int = 0
    private var controller1Buttons: Int = 0
    private var controller2Buttons: Int = 0

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
            in 0x4000 .. 0x4014 -> lastValueRead                         // APU channels (write only, reading open bus)
            0x4015 -> {                                                        // APU status (doesn't affect open bus)
                val apuStatus = apu.readStatus()
                return apuStatus or (lastValueRead and 0b00100000)            // Bit 5 is open bus
            }
            0x4016 -> {                                                       // Controller 1
                val nextButton = controller1Buttons and 0x01
                controller1Buttons = controller1Buttons ushr 1
                nextButton
            }
            0x4017 -> {                                                       // Controller 2
                val nextButton = controller2Buttons and 0x01
                controller2Buttons = controller2Buttons ushr 1
                nextButton
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
            else -> {
                Log.e(TAG, "Invalid CPU read at $${address.toHexString(4)}")
                throw InvalidOperationException("Invalid CPU read at $${address.toHexString(4)}")
            }
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
            in 0x4000 .. 0x4015, 0x4017 -> apu.writeRegister(address, value)    // APU registers
            0x4016 -> pollButtonStates()                                              // Controller
            0x4018, 0x4019 -> lastValueRead                                           // Unused? (open bus set for now)
            in 0x4020 .. 0x5FFF -> mapper.writeUnmappedRange(address, value)    // Usually unmapped
            in 0x6000 .. 0x7FFF -> mapper.writeRam(address, value)              // Usually cartridge SRAM
            in 0x8000 .. 0xFFFF -> mapper.writePrgRom(address, value)           // PRG-ROM
            else -> {
                Log.e(TAG, "Invalid CPU write at $${address.toHexString(4)}")
                throw InvalidOperationException("Invalid CPU write at $${address.toHexString(4)}")
            }
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
            else -> {
                Log.e(TAG, "Invalid PPU read at $${address.toHexString(4)}")
                throw InvalidOperationException("Invalid PPU read at $${address.toHexString(4)}")
            }
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
            else -> {
                Log.e(TAG, "Invalid PPU write at $${address.toHexString(4)}")
                throw InvalidOperationException("Invalid PPU write at $${address.toHexString(4)}")
            }
        }
    }

    fun insertCartridge(cartridge: Cartridge) {
        mapper = when (cartridge.mapperId) {
            0 -> Mapper0(cartridge)
            2 -> Mapper2(cartridge)
            3 -> Mapper3(cartridge)
            else -> throw RomParseException("Unsupported mapper: ${cartridge.mapperId}")
        }
        this.cartridge = cartridge
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
        apu.reset()

        var apuCycleCarry = 0
        val timeSource = TimeSource.Monotonic
        var fpsMeasureStart = timeSource.markNow()

        while (running) {
            val frameStart = timeSource.markNow()
            while (!isFrameReady) {
                var cpuCycles = cpu.step()

                for (i in 0 ..< cpuCycles * 3) {
                    ppu.tick()
                }

                cpuCycles += apuCycleCarry
                apuCycleCarry = cpuCycles % 2
                for (i in 0 ..< cpuCycles / 2) {
                    apu.tick()
                }
            }
            isFrameReady = false
            numFrames++

            val now = timeSource.markNow()
            // If we are lagging behind, the delta will be negative and the call to delay will
            // return immediately
            delay(1000 / TARGET_FPS - (now - frameStart).inWholeMilliseconds)

            if ((now - fpsMeasureStart).inWholeMilliseconds >= 3000) {
                fps = numFrames / 3f
                numFrames = 0
                fpsMeasureStart = timeSource.markNow()
                Log.i(TAG, "FPS: $fps")
            }
        }
    }

    fun pollButtonStates() {
        listeners.forEach { listener ->
            listener.onPollController1Buttons().let { it?.let { controller1Buttons = it or 0xFF00 } }
            listener.onPollController2Buttons().let { it?.let { controller2Buttons = it or 0xFF00 } }
        }
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
            in 0x4020 .. 0x5FFF -> 0                                    // Cartridge Expansion ROM
            in 0x6000 .. 0x7FFF -> 0                                    // SRAM
            in 0x8000 .. 0xFFFF -> mapper.readPrgRom(address)           // PRG-ROM
            else -> throw InvalidOperationException("Invalid CPU read at $address")
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