package com.onandor.nesemu.domain.emulation.nes.apu

import com.onandor.nesemu.domain.emulation.nes.cpu.Cpu
import com.onandor.nesemu.domain.emulation.nes.cpu.IRQSource
import com.onandor.nesemu.domain.emulation.nes.toInt
import com.onandor.nesemu.domain.emulation.savestate.ApuState
import com.onandor.nesemu.domain.emulation.savestate.Savable

class Apu(
    onReadMemory: (address: Int) -> Int,
    private val onSignalIRQ: (source: IRQSource, isRequest: Boolean) -> Unit,
    private val onAudioSampleReady: (Float) -> Unit
) : Savable<ApuState> {

    // Sample generation
    private var cpuCyclesPerSample: Int = Cpu.FREQUENCY_HZ / 4800
    private var cpuCyclesSinceSample: Int = 0

    // Frame counter
    private var sequenceCycles: Int = SEQ_4_STEP_CYCLES
    private var interrupt: Boolean = false
    private var interruptEnable: Boolean = false

    private var cpuCycles: Int = 0
    private var cycles: Int = 0

    // Channels
    private val pulse1 = PulseChannel(PulseChannel.CHANNEL_1)
    private val pulse2 = PulseChannel(PulseChannel.CHANNEL_2)
    private val triangle = TriangleChannel()
    private val noise = NoiseChannel()
    private val dmc = Dmc(onReadMemory, onSignalIRQ)

    // Mixer tables
    private val pulseOutputTable = FloatArray(31)
    private val tndOutputTable = FloatArray(203)

    init {
        pulseOutputTable[0] = 0.0f
        for (i in 1 .. 30) {
            pulseOutputTable[i] = 95.52f / (8128.0f / i + 100)
        }
        tndOutputTable[0] = 0.0f
        for (i in 1 .. 202) {
            tndOutputTable[i] = 163.67f / (24329.0f / i + 100)
        }
    }

    // Clocked every CPU cycle
    fun clock() {
        if (cycles >= sequenceCycles) {
            cycles = 0
            cpuCycles = 0
        }

        var isQuarterFrame = false
        var isHalfFrame = false

        when (cycles) {
            3728 -> {
                isQuarterFrame = true
            }
            7456 -> {
                isQuarterFrame = true
                isHalfFrame = true
            }
            11185 -> {
                isQuarterFrame = true
            }
            14914 -> {
                // Last step of 4 step sequence
                if (sequenceCycles == SEQ_4_STEP_CYCLES) {
                    isQuarterFrame = true
                    isHalfFrame = true
                    if (interruptEnable) {
                        interrupt = true
                        onSignalIRQ(IRQSource.ApuFrameCounter, true)
                    }
                }
            }
            18640 -> {
                // Last step of 5 step sequence
                isQuarterFrame = true
                isHalfFrame = true
            }
        }

        clockUnits(isQuarterFrame, isHalfFrame)

        if (cpuCycles % 2 == 0) {
            pulse1.clockTimer()
            pulse2.clockTimer()
            noise.clockTimer()
        }
        triangle.clockTimer()
        dmc.clockTimer()

        cpuCycles += 1
        cycles = cpuCycles / 2
        cpuCyclesSinceSample += 1

        if (cpuCyclesSinceSample >= cpuCyclesPerSample) {
            onAudioSampleReady(generateSample())
            cpuCyclesSinceSample = 0
        }
    }

    private fun clockUnits(isQuarterFrame: Boolean, isHalfFrame: Boolean) {
        if (isQuarterFrame) {
            pulse1.clockEnvelope()
            pulse2.clockEnvelope()
            triangle.clockLinearCounter()
            noise.clockEnvelope()
        }
        if (isHalfFrame) {
            pulse1.clockLengthCounter()
            pulse2.clockLengthCounter()
            pulse1.clockSweep()
            pulse2.clockSweep()
            triangle.clockLengthCounter()
            noise.clockLengthCounter()
        }
    }

    fun readStatus(): Int {
        val status = (pulse1.lengthCounter.halt).toInt() or
                (pulse2.lengthCounter.halt).toInt() shl 1 or
                (triangle.lengthCounter.halt).toInt() shl 2 or
                (noise.lengthCounter.halt).toInt() shl 3 or
                (dmc.reader.bytesRemaining > 0).toInt() shl 4 or
                interrupt.toInt() shl 6 or
                dmc.interrupt.toInt() shl 7
        onSignalIRQ(IRQSource.ApuFrameCounter, false)
        return status.also { interrupt = false }
    }

    fun writeRegister(address: Int, value: Int) {
        when (address) {
            0x4000 -> pulse1.writeControl(value)
            0x4001 -> pulse1.writeSweep(value)
            0x4002 -> pulse1.writeTimer(value)
            0x4003 -> pulse1.writeLengthCounter(value)
            0x4004 -> pulse2.writeControl(value)
            0x4005 -> pulse2.writeSweep(value)
            0x4006 -> pulse2.writeTimer(value)
            0x4007 -> pulse2.writeLengthCounter(value)
            0x4008 -> triangle.writeLinearCounter(value)
            0x400A -> triangle.writeTimer(value)
            0x400B -> triangle.writeLengthCounter(value)
            0x400C -> noise.writeControl(value)
            0x400E -> noise.writeTimer(value)
            0x400F -> noise.writeLengthCounter(value)
            0x4010 -> dmc.writeFlagsAndRate(value)
            0x4011 -> dmc.writeDirectLoad(value)
            0x4012 -> dmc.writeSampleAddress(value)
            0x4013 -> dmc.writeSampleLength(value)
            0x4015 -> {
                pulse1.writeEnabled(value and 0x01 != 0)
                pulse2.writeEnabled(value and 0x02 != 0)
                triangle.writeEnabled(value and 0x04 != 0)
                noise.writeEnabled(value and 0x08 != 0)
                dmc.writeEnabled(value and 0x10 != 0)
            }
            0x4017 -> {
                interruptEnable = value and 0x40 == 0
                if (!interruptEnable) {
                    interrupt = false
                    onSignalIRQ(IRQSource.ApuFrameCounter, false)
                }

                // The frame counter reset and the clocking of the units is supposed to happen
                // 2 or 3 cycles after the write, but this will do for now.
                cycles = 0
                sequenceCycles = if (value and 0x80 > 0) SEQ_5_STEP_CYCLES else SEQ_4_STEP_CYCLES
                if (sequenceCycles == SEQ_5_STEP_CYCLES) {
                    clockUnits(true, true)
                }
            }
        }
    }

    fun reset() {
        cpuCyclesSinceSample = 0
        sequenceCycles = SEQ_4_STEP_CYCLES
        interrupt = false
        interruptEnable = false
        cpuCycles = 0
        cycles = 0
        pulse1.reset()
        pulse2.reset()
        triangle.reset()
        noise.reset()
        dmc.reset()
    }

    fun setSampleRate(sampleRate: Int) {
        cpuCyclesPerSample = Cpu.FREQUENCY_HZ / sampleRate
    }

    private fun generateSample(): Float {
        val pulseSample = pulseOutputTable[pulse1.getOutput() + pulse2.getOutput()]
        val tndSample = tndOutputTable[3 * triangle.getOutput() + 2 * noise.getOutput() + dmc.getOutput()]
        return pulseSample + tndSample
    }

    override fun createSaveState(): ApuState {
        TODO("Not yet implemented")
    }

    override fun loadState(state: ApuState) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val SEQ_4_STEP_CYCLES = 14915
        private const val SEQ_5_STEP_CYCLES = 18641
    }
}