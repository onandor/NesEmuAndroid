package com.onandor.nesemu.domain.emulation.nes.apu2

import com.onandor.nesemu.domain.emulation.nes.Cpu
import com.onandor.nesemu.domain.emulation.nes.toInt
import com.onandor.nesemu.domain.emulation.savestate.ApuState
import com.onandor.nesemu.domain.emulation.savestate.Savable

class Apu(private val onAudioSampleReady: (Float) -> Unit) : Savable<ApuState> {

    // Sample generation
    private var cpuCyclesPerSample: Int = Cpu.FREQUENCY_HZ / 4800
    private var cpuCyclesSinceSample: Int = 0

    // Frame counter
    private var sequenceCycles: Int = SEQ_4_STEP_CYCLES

    private var cpuCycles: Int = 0
    private var cycles: Int = 0

    // Channels
    private val pulse1 = PulseChannel(PulseChannel.CHANNEL_1)
    private val pulse2 = PulseChannel(PulseChannel.CHANNEL_2)
    private val triangle = TriangleChannel()
    private val noise = NoiseChannel()

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
                if (sequenceCycles == SEQ_4_STEP_CYCLES) {
                    isQuarterFrame = true
                    isHalfFrame = true
                }
            }
            18640 -> {
                // Redundant check but helps in readability
                if (sequenceCycles == SEQ_5_STEP_CYCLES) {
                    isQuarterFrame = true
                    isHalfFrame = true
                }
            }
        }

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

        if (cpuCycles % 2 == 0) {
            pulse1.clockTimer()
            pulse2.clockTimer()
            noise.clockTimer()
        }
        triangle.clockTimer()

        cpuCycles += 1
        cycles = cpuCycles / 2
        cpuCyclesSinceSample += 1

        if (cpuCyclesSinceSample >= cpuCyclesPerSample) {
            onAudioSampleReady(generateSample())
            cpuCyclesSinceSample = 0
        }
    }

    fun readStatus(): Int {
        // TODO
        return (pulse1.getLength() > 0).toInt() or
                (pulse2.getLength() > 0).toInt() shl 1
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
            0x4015 -> {
                pulse1.setEnabled(value and 0x01 != 0)
                pulse2.setEnabled(value and 0x02 != 0)
                triangle.setEnabled(value and 0x04 != 0)
                noise.setEnabled(value and 0x08 != 0)
            }
            0x4017 -> {
                sequenceCycles = if (value and 0x80 > 0) SEQ_5_STEP_CYCLES else SEQ_4_STEP_CYCLES
            }
        }
    }

    fun reset() {
        cpuCyclesSinceSample = 0
        sequenceCycles = SEQ_4_STEP_CYCLES
        cpuCycles = 0
        cycles = 0
        pulse1.reset()
        pulse2.reset()
        triangle.reset()
    }

    fun setSampleRate(sampleRate: Int) {
        cpuCyclesPerSample = Cpu.FREQUENCY_HZ / sampleRate
    }

    private fun generateSample(): Float {
        val pulseSample = pulseOutputTable[pulse1.getOutput() + pulse2.getOutput()]
        val tndSample = tndOutputTable[3 * triangle.getOutput() + 2 * noise.getOutput()]
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