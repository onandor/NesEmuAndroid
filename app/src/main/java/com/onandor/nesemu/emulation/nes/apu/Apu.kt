package com.onandor.nesemu.emulation.nes.apu

import com.onandor.nesemu.emulation.nes.Cpu

class Apu(
    onGenerateIRQ: () -> Unit,
    onReadMemory: (Int) -> Int,
    private val onAudioSampleReady: (Float) -> Unit
) : Clockable {

    private var sampleRate: Int = 48000
    private var cpuCyclesPerSample: Int = Cpu.FREQUENCY_HZ / sampleRate
    private var cpuCyclesSinceSample: Int = 0
    private var cycles: Int = 0
    private var cpuCycles: Int = 0
    private var sequenceCycles: Int = SEQ_4_STEP_CYCLES

    private val pulse1 = PulseChannel(PULSE_CHANNEL_1)
    private val pulse2 = PulseChannel(PULSE_CHANNEL_2)
    private val triangle = TriangleChannel()
    private val noise = NoiseChannel()
    private val dmc = DMC(onGenerateIRQ, onReadMemory)

    private val pulseTable = FloatArray(31)
    private val tndTable = FloatArray(203)

    init {
        for (i in 0 until 31) {
            pulseTable[i] = 95.52f / (8128f / i.toFloat() + 100f)
        }
        for (i in 0 until 203) {
            tndTable[i] = 163.67f / (24329f / i.toFloat() + 100f)
        }
    }

    // Clocks the frame counter's looping sequencer
    // Called every CPU cycle
    override fun clock() {
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
                isQuarterFrame = sequenceCycles == SEQ_4_STEP_CYCLES
                isHalfFrame = sequenceCycles == SEQ_4_STEP_CYCLES
            }
            18640 -> {
                isQuarterFrame = true
                isHalfFrame = true
            }
        }

        if (isQuarterFrame) {
            pulse1.envelope.clock()
            pulse2.envelope.clock()
            triangle.clockCounter()
            noise.envelope.clock()
        }
        if (isHalfFrame) {
            pulse1.clock()
            pulse2.clock()
            triangle.clock()
            noise.clock()
            pulse1.sweep.clock()
            pulse2.sweep.clock()
        }
        if (cpuCycles % 2 == 0) {
            pulse1.divider.clock()
            pulse2.divider.clock()
            noise.divider.clock()
            dmc.divider.clock()
        }
        triangle.divider.clock()

        cpuCycles += 1
        cpuCyclesSinceSample += 1
        if (cpuCyclesSinceSample >= cpuCyclesPerSample) {
            onAudioSampleReady(getSample())
            cpuCyclesSinceSample = 0
        }

        cycles = cpuCycles / 2
        if (cycles >= sequenceCycles) {
            cycles = 0
            cpuCycles = 0
        }
    }

    fun readStatus(): Int {
        return 0 // TODO
    }

    fun writeRegister(address: Int, value: Int) {
        when (address) {
            0x4000 -> pulse1.setControl(value)
            0x4001 -> pulse1.setSweep(value)
            0x4002 -> pulse1.setDividerLow(value)
            0x4003 -> pulse1.setDividerHigh(value)
            0x4004 -> pulse2.setControl(value)
            0x4005 -> pulse2.setSweep(value)
            0x4006 -> pulse2.setDividerLow(value)
            0x4007 -> pulse2.setDividerHigh(value)
            0x4008 -> triangle.setControl(value)
            0x400A -> triangle.setDividerLow(value)
            0x400B -> triangle.setDividerHigh(value)
            0x400C -> noise.setControl(value)
            0x400E -> noise.setDivider(value)
            0x400F -> noise.setLengthAndEnvelope(value)
            0x4010 -> dmc.setControl(value)
            0x4011 -> dmc.setDirectLoad(value)
            0x4012 -> dmc.setSampleAddress(value)
            0x4013 -> dmc.setSampleLength(value)
            0x4015 -> {
                pulse1.lengthFrozen = value and 0x01 == 0
                if (pulse1.lengthFrozen) {
                    pulse1.length = 0
                }
                pulse2.lengthFrozen = value and 0x02 == 0
                if (pulse2.lengthFrozen) {
                    pulse2.length = 0
                }
                triangle.control = value and 0x04 == 0
                if (triangle.control) {
                    triangle.length = 0
                }
                noise.lengthFrozen = value and 0x08 == 0
                if (noise.lengthFrozen) {
                    noise.length = 0
                }
                dmc.isEnabled = value and 0x10 > 0
                if (!dmc.isEnabled) {
                    dmc.sample.bytesRemaining = 0
                } else {
                    // TODO: If the DMC bit is set, the DMC sample will be restarted only if its bytes
                    //  remaining is 0. If there are bits remaining in the 1-byte sample buffer, these
                    //  will finish playing before the next sample is fetched.
                    if (dmc.sample.bytesRemaining == 0) {
                        dmc.sample.address = dmc.sample.startingAddress
                        dmc.sample.bytesRemaining = dmc.sample.length
                    }

                    // TODO: Writing to this register clears the DMC interrupt flag.
                }
            }
            0x4017 -> {
                sequenceCycles = if (value and 0x80 == 0) SEQ_4_STEP_CYCLES else SEQ_5_STEP_CYCLES
                if (sequenceCycles == SEQ_5_STEP_CYCLES) {
                    pulse1.clock()
                    pulse2.clock()
                    triangle.clock()
                    triangle.clockCounter()
                    noise.clock()
                    pulse1.envelope.clock()
                    pulse2.envelope.clock()
                    pulse1.sweep.clock()
                    pulse2.sweep.clock()
                }
            }
        }
    }

    override fun reset() {
        cycles = 0
        cpuCycles = 0
        cpuCyclesSinceSample = 0
        sequenceCycles = SEQ_4_STEP_CYCLES

        pulse1.reset()
        pulse2.reset()
    }

    fun setSampleRate(newSampleRate: Int) {
        sampleRate = newSampleRate
        cpuCyclesPerSample = Cpu.FREQUENCY_HZ / sampleRate
    }

    private fun getSample(): Float {
        val pulseSample = 0.00752f * (pulse1.getOutput() + pulse2.getOutput())
        val tndSample = 0.00851f * triangle.getOutput() + 0.00494f * noise.getOutput() + 0.00335f * dmc.getOutput()
        //val pulseSample = pulseTable[pulse1.getOutput() + pulse2.getOutput()]
        //val tndSample = tndTable[3 * triangle.getOutput() + 2 * noise.getOutput() + dmc.getOutput()]
        return pulseSample + tndSample
    }

    companion object {
        private const val SEQ_4_STEP_CYCLES = 14915
        private const val SEQ_5_STEP_CYCLES = 18641

        const val PULSE_CHANNEL_1 = 1
        const val PULSE_CHANNEL_2 = 2

        val LENGTH_COUNTER_LOOKUP: Array<Int> = arrayOf(
            10, 254, 20,  2, 40,  4, 80,  6, 160,  8, 60, 10, 14, 12, 26, 14,
            12,  16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
        )
    }
}