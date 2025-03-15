package com.onandor.nesemu.emulation.nes

import android.util.Log
import androidx.collection.mutableFloatListOf
import com.onandor.nesemu.emulation.nes.apu.Apu
import com.onandor.nesemu.emulation.nes.mappers.Mapper
import com.onandor.nesemu.emulation.nes.mappers.Mapper0
import com.onandor.nesemu.emulation.nes.mappers.Mapper2
import com.onandor.nesemu.emulation.nes.mappers.Mapper3
import com.onandor.nesemu.emulation.nes.ppu.Ppu
import com.onandor.nesemu.emulation.savestate.NesState
import com.onandor.nesemu.emulation.savestate.Savable
import com.onandor.nesemu.util.SlidingWindowIntQueue
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

class Nes(
    private val onFrameReady: (
        frame: IntArray,
        patternTable: IntArray,
        nametable: IntArray,
        colorPalettes: Array<IntArray>) -> Unit,
    private val onPollController1: () -> Int,
    private val onPollController2: () -> Int
) : Savable<NesState> {

    private companion object {
        const val TAG = "Nes"
        const val MEMORY_SIZE = 2048
        const val TARGET_FPS = 60
    }

    private var cpuMemory: IntArray = IntArray(MEMORY_SIZE)
    private var vram: IntArray = IntArray(MEMORY_SIZE)
    val cpu: Cpu = Cpu(::cpuReadMemory, ::cpuWriteMemory)
    val ppu: Ppu = Ppu(::ppuReadMemory, ::ppuWriteMemory, cpu::NMI, ::ppuFrameReady)
    val apu: Apu = Apu(cpu::IRQ, ::apuReadMemory, ::apuSampleReady)
    private var cartridge: Cartridge? = null
    private lateinit var mapper: Mapper

    private var lastValueRead: Int = 0
    private var controller1Buttons: Int = 0
    private var controller2Buttons: Int = 0

    private var audioBuffer = mutableFloatListOf()
    private val audioSampleSizeQueue = SlidingWindowIntQueue(100)
    private var targetAudioBufferSize: Int = 0

    var running: Boolean = false
        private set
    private var isFrameReady: Boolean = false
    private var numFrames: Int = 0
    var fps: Float = 0f
        private set

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
                val value = mapper.readPrgRam(address)
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
                // TODO: stall - https://www.nesdev.org/wiki/PPU_OAM#DMA
                ppu.loadOamData(oamData)
            }
            in 0x4000 .. 0x4015, 0x4017 -> apu.writeRegister(address, value)    // APU registers
            0x4016 -> {                                                               // Controllers
                controller1Buttons = onPollController1() or 0xFF00
                controller2Buttons = onPollController2() or 0xFF00
            }
            0x4018, 0x4019 -> lastValueRead                                           // Unused? (open bus set for now)
            in 0x4020 .. 0x5FFF -> mapper.writeUnmappedRange(address, value)    // Usually unmapped
            in 0x6000 .. 0x7FFF -> mapper.writePrgRam(address, value)           // Usually cartridge SRAM
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

    private fun ppuFrameReady(
        frame: IntArray,
        patternTable: IntArray,
        nametable: IntArray,
        colorPalettes: Array<IntArray>
    ) {
        isFrameReady = true
        onFrameReady(frame, patternTable, nametable, colorPalettes)
    }

    fun apuReadMemory(address: Int): Int {
        cpu.stall(4)
        return mapper.readPrgRom(address)   // Address is between 0x8000 - 0xFFFF
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

    private fun apuSampleReady(sample: Float) {
        audioBuffer.add(sample)
    }

    fun drainAudioBuffer(numSamples: Int): FloatArray {
//        if (!audioSampleSizeQueue.isFull()) {
//            audioSampleSizeQueue.add(numSamples)
//        } else if (targetAudioBufferSize == 0) {
//            targetAudioBufferSize = (audioSampleSizeQueue.average * 3).toInt()
//        }

        val size = if (audioBuffer.size < numSamples) audioBuffer.size else numSamples
        val samples = FloatArray(size)
        for (i in 0 ..< size) {
            samples[i] = audioBuffer[i]
        }
        audioBuffer.removeRange(0, size)

//        if (targetAudioBufferSize != 0) {
//            if (audioBuffer.size > targetAudioBufferSize * 1.5) {
//                val ratio = (audioBuffer.size - targetAudioBufferSize).toFloat() / audioBuffer.size
//                apu.sampleRate -= (0.01 * ratio * apu.sampleRate).toInt()
//            } else if (audioBuffer.size < targetAudioBufferSize) {
//                val ratio = (targetAudioBufferSize - audioBuffer.size).toFloat() / targetAudioBufferSize
//                apu.sampleRate += (0.02 * ratio * apu.sampleRate).toInt()
//            }
//        }

        return samples
    }

    fun reset() {
        cpuMemory = IntArray(MEMORY_SIZE)
        vram = IntArray(MEMORY_SIZE)
        lastValueRead = 0
        numFrames = 0
        isFrameReady = false
        audioBuffer.clear()
        audioSampleSizeQueue.clear()
        controller1Buttons = 0
        controller2Buttons = 0

        cpu.reset()
        ppu.reset()
        apu.reset()
        cartridge!!.reset()
    }

    suspend fun run() {
        running = true
        val timeSource = TimeSource.Monotonic
        var fpsMeasureStart = timeSource.markNow()

        while (running) {
            val frameStart = timeSource.markNow()
            while (!isFrameReady) {
                var cpuCycles = cpu.step()

                for (i in 0 ..< cpuCycles * 3) {
                    ppu.tick()
                }

                for (i in 0 ..< cpuCycles) {
                    apu.clock()
                }
            }
            isFrameReady = false
            numFrames += 1

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

    fun stop() {
        running = false
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

    override fun saveState(): NesState {
        return NesState(
            cpuMemory = cpuMemory.copyOf(),
            lastValueRead = lastValueRead,
            vram = vram.copyOf(),
            cpu = cpu.saveState(),
            ppu = ppu.saveState(),
            apu = apu.saveState(),
            cartridge = cartridge!!.saveState(),
            mapper = mapper.saveState()
        )
    }

    override fun loadState(state: NesState) {
        cpuMemory = state.cpuMemory.copyOf()
        lastValueRead = state.lastValueRead
        vram = state.vram.copyOf()
        cpu.loadState(state.cpu)
        ppu.loadState(state.ppu)
        apu.loadState(state.apu)
        cartridge!!.loadState(state.cartridge)
        mapper.loadState(state.mapper)
    }
}