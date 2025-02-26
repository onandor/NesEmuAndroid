package com.onandor.nesemu.emulation.nes

import android.util.Log
import kotlin.random.Random

class Apu(
    private val generateIRQ: () -> Unit,
    private val onAudioSampleReady: (Float) -> Unit
) {

    private enum class SequencerMode(val cycles: Int) {
        MODE_4_STEP(14915),
        MODE_5_STEP(18641)
    }

    private open class WaveformChannel {
        var length: Int = 0
        var lengthCounterHalted: Boolean = false
        var timer: Int = 0

        var frequency: Double = 0.0
        var output: Int = 0 // 0 - 15

        open fun reset() {
            length = 0
            lengthCounterHalted = false
            timer = 0
            output = 0
        }

        open fun frameCounterTick() {
            if (!lengthCounterHalted) {
                if (length > 0) {
                    length -= 1
                } else {
                    // TODO: clock waveform generator
                    length = timer
                }
            }
        }
    }

    private companion object {
        const val TAG = "Apu"

        val LENGTH_COUNTER_LOOKUP: Array<Int> = arrayOf(
            10, 254, 20,  2, 40,  4, 80,  6, 160,  8, 60, 10, 14, 12, 26, 14,
            12,  16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
        )

        val PULSE_DUTY_CYCLE_LOOKUP: Array<Int> = arrayOf(
        //  12.5%       25%         50%         75% (25% negated)
            0b01000000, 0b01100000, 0b01111000, 0b10011111
        )
    }

    private object StatusFlags {
        const val DMC_ENABLE: Int = 0b00010000
        const val NOISE_ENABLE: Int = 0b00001000
        const val TRIANGLE_ENABLE: Int = 0b00000100
        const val PULSE_2_ENABLE: Int = 0b00000010
        const val PULSE_1_ENABLE: Int = 0b00000001
    }

    private object FrameCounterFlags {
        const val MODE: Int = 0b10000000
        const val IRQ_INHIBIT: Int = 0b01000000
    }

    private object Pulse1 : WaveformChannel() {
        var dutyCycle: Int = PULSE_DUTY_CYCLE_LOOKUP[0]

        override fun reset() {
            super.reset()
            dutyCycle = PULSE_DUTY_CYCLE_LOOKUP[0]
        }
    }

    private object Pulse2 : WaveformChannel() {
        var dutyCycle: Int = PULSE_DUTY_CYCLE_LOOKUP[0]

        override fun reset() {
            super.reset()
            dutyCycle = PULSE_DUTY_CYCLE_LOOKUP[0]
        }
    }

    private object Triangle : WaveformChannel() {
        // TODO: might need to override frameCounterTick
        // https://www.nesdev.org/wiki/APU_Frame_Counter - Mode 0 and mode 1
    }

    private object Noise : WaveformChannel() {

    }

    private object DMC {
        var bytesRemaining: Int = 0
        var interruptOccurred: Boolean = false
        var output: Int = 0 // 0 - 127

        fun reset() {
            bytesRemaining = 0
            interruptOccurred = false
            output = 0
        }
    }

    var sampleRate: Int = 44100
        set(value) {
            cpuCyclesPerSample = Cpu.FREQUENCY / value
            field = value
        }
    private var cpuCyclesPerSample: Int = Cpu.FREQUENCY / sampleRate
    private var cpuCyclesSinceSample: Int = 0
    private var cycles: Int = 0
    private var cpuCycles: Int = 0

    private var interruptOccurred: Boolean = false
    private var interruptInhibited: Boolean = false
    private var sequencerMode: SequencerMode = SequencerMode.MODE_4_STEP

    // Clocks the frame counter's looping sequencer
    // Called every 2 CPU cycles
    fun tick() {
        // TODO: multiple things missing
        // https://www.nesdev.org/wiki/APU_Frame_Counter - Mode 0 and mode 1
        when (cycles) {
            3728 -> {                                                   // Step 1
                Triangle.frameCounterTick()
            }
            7456 -> {                                                   // Step 2
                Pulse1.frameCounterTick()
                Pulse2.frameCounterTick()
                Triangle.frameCounterTick()
            }
            11185 -> {                                                  // Step 3
                Triangle.frameCounterTick()
            }
            14914 -> {                                                  // Step 4
                if (sequencerMode == SequencerMode.MODE_4_STEP) {
                    Pulse1.frameCounterTick()
                    Pulse2.frameCounterTick()
                    Triangle.frameCounterTick()
                    interruptOccurred = !interruptInhibited
                    if (!interruptInhibited) {
                        interruptOccurred = true
                        generateIRQ() // TODO: might not be the most accurate mode to generate an IRQ (the same goes for the PPU)
                    }
                }
            }
            18640 -> {                                                  // Step 5
                Pulse1.frameCounterTick()
                Pulse2.frameCounterTick()
                Triangle.frameCounterTick()
            }
        }

        cpuCycles += 1
        cpuCyclesSinceSample += 1
        if (cpuCyclesSinceSample >= cpuCyclesPerSample) {
            onAudioSampleReady(getSample())
            cpuCyclesSinceSample = 0
        }

        cycles = cpuCycles / 2
        if (cycles >= sequencerMode.cycles) {
            cycles = 0
            cpuCycles = 0
        }
    }

    fun readStatus(): Int {
        val prevInterruptOccurred = interruptOccurred
        interruptOccurred = false
        return (DMC.interruptOccurred.toInt() shl 7) or
                (prevInterruptOccurred.toInt() shl 6) or
                0 or
                ((DMC.bytesRemaining > 0).toInt() shl 4) or
                ((Noise.length > 0).toInt() shl 3) or
                ((Triangle.length > 0).toInt() shl 2) or
                ((Pulse2.length > 0).toInt() shl 1) or
                (Pulse1.length > 0).toInt()
    }

    fun writeRegister(address: Int, value: Int) {
        DMC.interruptOccurred = false
        when (address) {
            0x4000 -> {
                // TODO: might not be correct with the length counter
                // https://www.nesdev.org/wiki/APU#Pulse_($4000%E2%80%93$4007)
                Pulse1.dutyCycle = PULSE_DUTY_CYCLE_LOOKUP[(value and 0xC0) ushr 6]
                Pulse1.lengthCounterHalted = value and 0x20 > 0
            }
            0x4001 -> {} // TODO: pulse 1 sweep
            0x4002 -> {
                Pulse1.timer = Pulse1.timer or value
            }
            0x4003 -> {
                Pulse1.timer = Pulse1.timer or ((value and 0b111) shl 10)
                Pulse1.length = LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
            }
            0x4004 -> {
                Pulse2.dutyCycle = PULSE_DUTY_CYCLE_LOOKUP[(value and 0xC0) ushr 6]
                Pulse2.lengthCounterHalted = value and 0x20 > 0
            }
            0x4005 -> {} // TODO: pulse 2 sweep
            0x4006 -> {
                Pulse2.timer = Pulse2.timer or value
            }
            0x4007 -> {
                Pulse2.timer = Pulse2.timer or ((value and 0b111) shl 10)
                Pulse2.length = LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
            }
            0x400B -> {
                Triangle.length = LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
            }
            0x400F -> {
                Noise.length = LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
            }
            0x4015 -> {
                Pulse1.lengthCounterHalted = value and StatusFlags.PULSE_1_ENABLE == 0
                if (Pulse1.lengthCounterHalted) {
                    Pulse1.length = 0
                }
                Pulse2.lengthCounterHalted = value and StatusFlags.PULSE_2_ENABLE == 0
                if (Pulse2.lengthCounterHalted) {
                    Pulse2.length = 0
                }
                Triangle.lengthCounterHalted = value and StatusFlags.TRIANGLE_ENABLE == 0
                if (Triangle.lengthCounterHalted) {
                    Triangle.length = 0
                }
                Noise.lengthCounterHalted = value and StatusFlags.NOISE_ENABLE == 0
                if (Noise.lengthCounterHalted) {
                    Noise.length = 0
                }
                if (value and StatusFlags.DMC_ENABLE == 0) {
                    // TODO: doesn't immediately silence, but only after it finishes the current playback
                    DMC.bytesRemaining = 0
                } else {
                    // TODO: restart if the remaining length is 0
                    // https://www.nesdev.org/wiki/APU#Status_($4015)
                }
            }
            0x4017 -> {
                sequencerMode = if (value and FrameCounterFlags.MODE == 0) {
                    SequencerMode.MODE_4_STEP
                } else {
                    SequencerMode.MODE_5_STEP
                }
                interruptInhibited = value and FrameCounterFlags.IRQ_INHIBIT > 0
                if (interruptInhibited) {
                    interruptOccurred = false
                }
            }
            else -> Log.w(TAG, "Write to unknown address: 0x${address.toHexString(4)} " +
                    "(value: 0x${value.toHexString(4)})")
        }
    }

    fun reset() {
        cycles = 0
        cpuCycles = 0
        cpuCyclesSinceSample = 0
        interruptOccurred = false
        interruptInhibited = false
        sequencerMode = SequencerMode.MODE_4_STEP

        Pulse1.reset()
        Pulse2.reset()
        Triangle.reset()
        Noise.reset()
        DMC.reset()
    }

    fun getSample(): Float {
        val pulseSample = 0.00752f * (Pulse1.output + Pulse2.output)
        val tndSample = 0.00851f * Triangle.output + 0.00494f * Noise.output + 0.00335f * DMC.output
        return pulseSample + tndSample
        //return (Random.nextFloat() - 0.5f) * 2.0f * 0.05f
    }
}