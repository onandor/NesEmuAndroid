package com.onandor.nesemu.domain.emulation.nes

import android.util.Log
import androidx.collection.mutableFloatListOf
import com.onandor.nesemu.domain.emulation.nes.apu.Apu
import com.onandor.nesemu.domain.emulation.nes.cpu.Cpu
import com.onandor.nesemu.domain.emulation.nes.mappers.*
import com.onandor.nesemu.domain.emulation.nes.ppu.Ppu
import com.onandor.nesemu.domain.emulation.savestate.NesState
import com.onandor.nesemu.domain.emulation.savestate.Savable
import com.onandor.nesemu.util.SlidingWindowIntQueue

class Nes(
    private val onPollController1: () -> Int,
    private val onPollController2: () -> Int
) : Savable<NesState> {

    data class Frame(
        val frame: IntArray,
        val patternTable: IntArray,
        val nametable: IntArray,
        val colorPalettes: Array<IntArray>
    )

    private companion object {
        const val TAG = "Nes"
        const val MEMORY_SIZE = 2048
        const val TARGET_FPS = 60
    }

    private var cpuMemory: IntArray = IntArray(MEMORY_SIZE)
    private var vram: IntArray = IntArray(MEMORY_SIZE)
    val cpu: Cpu = Cpu(::cpuReadMemory, ::cpuWriteMemory)
    val ppu: Ppu = Ppu(::ppuReadMemory, ::ppuWriteMemory, cpu::signalNMI, ::ppuFrameReady)
    val apu: Apu = Apu(::apuReadMemory, cpu::signalIRQ, ::apuSampleReady)
    private var cartridge: Cartridge? = null
    private lateinit var mapper: Mapper

    private var lastValueRead: Int = 0
    private var controller1Buttons: Int = 0
    private var controller2Buttons: Int = 0

    var frame: Frame? = null
        private set
    private var audioBuffer = mutableFloatListOf()
    private val audioSampleSizeQueue = SlidingWindowIntQueue(100)
    private var targetAudioBufferSize: Int = 0

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
                if (value == Mapper.OPEN_BUS) lastValueRead else value
            }
            in 0x6000 .. 0x7FFF -> {                                    // Usually cartridge SRAM
                val value = mapper.readPrgRam(address)
                if (value == Mapper.OPEN_BUS) lastValueRead else value
            }
            in 0x8000 .. 0xFFFF -> mapper.readPrgRom(address)           // PRG-ROM
            else -> Log.w(TAG, "CPU reading invalid address $${address.toHexString(4)}")
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
                Log.w(TAG, "CPU writing invalid address $${address.toHexString(4)} " +
                        "(value: $${value.toHexString(2)})")
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
            else -> Log.w(TAG, "PPU reading invalid address $${address.toHexString(4)}")
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
                Log.w(TAG, "PPU writing invalid address $${address.toHexString(4)} " +
                        "(value: $${value.toHexString(2)})")
            }
        }
    }

    private fun ppuFrameReady(
        frame: IntArray,
        patternTable: IntArray,
        nametable: IntArray,
        colorPalettes: Array<IntArray>
    ) {
        this.frame = Frame(
            frame = frame,
            patternTable = patternTable,
            nametable = nametable,
            colorPalettes = colorPalettes
        )
    }

    fun apuReadMemory(address: Int): Int {
        cpu.stall(4)
        return mapper.readPrgRom(address)   // Address is between 0x8000 - 0xFFFF
    }

    fun insertCartridge(cartridge: Cartridge) {
        mapper = when (cartridge.mapperId) {
            0 -> Mapper0(cartridge)
            1 -> Mapper1(cartridge)
            2 -> Mapper2(cartridge)
            3 -> Mapper3(cartridge)
            71 -> Mapper71(cartridge)
            else -> throw RomParseException("Unsupported mapper: ${cartridge.mapperId}")
        }
        this.cartridge = cartridge
    }

    private fun apuSampleReady(sample: Float) {
        audioBuffer.add(sample)
    }

    fun drainAudioBuffer(): FloatArray {
//        if (!audioSampleSizeQueue.isFull()) {
//            audioSampleSizeQueue.add(numSamples)
//        } else if (targetAudioBufferSize == 0) {
//            targetAudioBufferSize = (audioSampleSizeQueue.average * 3).toInt()
//        }

        val size = audioBuffer.size
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
        frame = null
        audioBuffer.clear()
        audioSampleSizeQueue.clear()
        controller1Buttons = 0
        controller2Buttons = 0

        cpu.reset()
        ppu.reset()
        apu.reset()
        cartridge!!.reset()
        mapper.reset()
    }

    fun generateFrame(): Frame {
        while (frame == null) {
            val cpuCycles = cpu.clock()

            for (i in 0 ..< cpuCycles * 3) {
                ppu.tick()
            }

            for (i in 0 ..< cpuCycles) {
                apu.clock()
            }
        }
        return frame!!.also { frame = null }
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
            DebugFeature.PpuRenderPatternTable -> {
                ppu.dbgDrawPatternTable = value
            }
            DebugFeature.PpuRenderNametable -> {
                ppu.dbgDrawNametable = value
            }
            DebugFeature.PpuRenderColorPalettes -> {
                ppu.dbgDrawColorPalettes = value
            }
            else -> {}
        }
    }

    fun setDebugFeatureInt(feature: DebugFeature, value: Int) {
        when (feature) {
            DebugFeature.PpuSetColorPalette -> {
                ppu.dbgColorPaletteId = value
            }
            else -> {}
        }
    }

    override fun captureState(): NesState {
        return NesState(
            cpuMemory = cpuMemory.copyOf(),
            lastValueRead = lastValueRead,
            vram = vram.copyOf(),
            cpu = cpu.captureState(),
            ppu = ppu.captureState(),
            apu = apu.captureState(),
            cartridge = cartridge!!.captureState(),
            mapper = mapper.captureState()
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