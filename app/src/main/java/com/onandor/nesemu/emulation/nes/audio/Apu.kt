package com.onandor.nesemu.emulation.nes.audio

import com.onandor.nesemu.emulation.nes.Cpu
import com.onandor.nesemu.emulation.nes.toInt

class Apu(
    private val generateIRQ: () -> Unit,
    private val onAudioSampleReady: (Float) -> Unit
) {

    private enum class SequencerMode(val cycles: Int) {
        MODE_4_STEP(14915),
        MODE_5_STEP(18641)
    }

    private class PulseChannel {
        var length: Int = 0
        var lengthCounterHalted: Boolean = false
        // 11 bit timer, counts t, t-1, ..., 0, t, t-1, ...
        // Clocks the waveform generator when it goes from 0 to t
        var t: Int = 0
        var tReload: Int = 0
        var dutyCycle: Int = PULSE_DUTY_CYCLE_LOOKUP[0]
        var phase: Int = 0

        var volume: Int = 0 // 0 - 15

        val output: Int
            get() {
                // TODO: sweep unit's adder overflows -> silence
                return if ((dutyCycle shl phase) and 0x80 == 0 || length == 0 || t < 8) {
                    0
                } else {
                    volume
                }
            }

        fun reset() {
            length = 0
            lengthCounterHalted = false
            t = 0
            tReload = 0
            dutyCycle = PULSE_DUTY_CYCLE_LOOKUP[0]
            phase = 0
        }

        fun tick() {
            // Clocking the length counter
            if (!lengthCounterHalted && length > 0) {
                length -= 1
            }
        }

        fun tickTimer() {
            if (t > 0) {
                t -= 1
            } else {
                t = tReload
                phase = (phase + 1) % 8
            }
        }
    }

    private class Envelope {
        var start: Boolean = false
        var volume: Int = 0


        fun clock() {

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

    private val pulse1 = PulseChannel()
    private val pulse2 = PulseChannel()

    var sampleRate: Int = 44100
        set(value) {
            cpuCyclesPerSample = Cpu.Companion.FREQUENCY_HZ / value
            field = value
        }
    private var cpuCyclesPerSample: Int = Cpu.Companion.FREQUENCY_HZ / sampleRate
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
                //Triangle.tick()
            }
            7456 -> {                                                   // Step 2
                pulse1.tick()
                pulse2.tick()
                //Triangle.tick()
            }
            11185 -> {                                                  // Step 3
                //Triangle.tick()
            }
            14914 -> {                                                  // Step 4
                if (sequencerMode == SequencerMode.MODE_4_STEP) {
                    pulse1.tick()
                    pulse2.tick()
                    //Triangle.tick()
                    if (!interruptInhibited) {
                        interruptOccurred = true
                        generateIRQ() // TODO: might not be the most accurate mode to generate an IRQ (the same goes for the PPU)
                    } else {
                        interruptOccurred = false
                    }
                }
            }
            18640 -> {                                                  // Step 5 (only reached in 5 step mode)
                pulse1.tick()
                pulse2.tick()
                //Triangle.tick()
            }
        }

        if (cpuCycles % 2 == 0) {
            pulse1.tickTimer()
            pulse2.tickTimer()
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
                //((Noise.length > 0).toInt() shl 3) or
                //((Triangle.length > 0).toInt() shl 2) or
                0 or
                0 or
                ((pulse2.length > 0).toInt() shl 1) or
                (pulse1.length > 0).toInt()
    }

    fun writeRegister(address: Int, value: Int) {
        DMC.interruptOccurred = false
        when (address) {
            0x4000 -> {
                pulse1.dutyCycle = PULSE_DUTY_CYCLE_LOOKUP[(value and 0xC0) ushr 6]
                pulse1.lengthCounterHalted = value and 0x20 > 0
                if (value and 0x10 > 0) {
                    // Constant volume
                    pulse1.volume = value and 0x0F
                } else {
                    // TODO: set envelope lowering rate
                }
            }
            0x4001 -> {} // TODO: pulse 1 sweep
            0x4002 -> {
                pulse1.tReload = (pulse1.tReload and 0x700) or (value and 0xFF)
            }
            0x4003 -> {
                pulse1.tReload = ((value and 0b111) shl 8) or (pulse1.tReload and 0xFF)
                pulse1.t = pulse1.tReload
                pulse1.length = LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
                pulse1.phase = 0
                // TODO: restart envelope
            }
            0x4004 -> {
                pulse2.dutyCycle = PULSE_DUTY_CYCLE_LOOKUP[(value and 0xC0) ushr 6]
                pulse2.lengthCounterHalted = value and 0x20 > 0
                if (value and 0x10 > 0) {
                    // Constant volume
                    pulse2.volume = value and 0x0F
                } else {
                    // TODO: set envelope lowering rate
                }
            }
            0x4005 -> {} // TODO: pulse 2 sweep
            0x4006 -> {
                pulse2.tReload = (pulse2.tReload and 0x700) or (value and 0xFF)
            }
            0x4007 -> {
                pulse2.tReload = ((value and 0b111) shl 8) or (pulse2.tReload and 0xFF)
                pulse2.t = pulse2.tReload
                pulse2.length = LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
                pulse2.phase = 0
                // TODO: restart envelope
            }
            0x400B -> {
//                if (!Triangle.lengthCounterHalted) {
//                    Triangle.length = LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
//                }
            }
            0x400F -> {
//                if (!Noise.lengthCounterHalted) {
//                    Noise.length = LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
//                }
            }
            0x4015 -> {
                pulse1.lengthCounterHalted = value and StatusFlags.PULSE_1_ENABLE == 0
                if (pulse1.lengthCounterHalted) {
                    pulse1.length = 0
                }
                pulse2.lengthCounterHalted = value and StatusFlags.PULSE_2_ENABLE == 0
                if (pulse2.lengthCounterHalted) {
                    pulse2.length = 0
                }
//                Triangle.lengthCounterHalted = value and StatusFlags.TRIANGLE_ENABLE == 0
//                if (Triangle.lengthCounterHalted) {
//                    Triangle.length = 0
//                }
//                Noise.lengthCounterHalted = value and StatusFlags.NOISE_ENABLE == 0
//                if (Noise.lengthCounterHalted) {
//                    Noise.length = 0
//                }
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
            else -> {}
//            else -> Log.w(TAG, "Write to unknown address: 0x${address.toHexString(4)} " +
//                    "(value: 0x${value.toHexString(4)})")
        }
    }

    fun reset() {
        cycles = 0
        cpuCycles = 0
        cpuCyclesSinceSample = 0
        interruptOccurred = false
        interruptInhibited = false
        sequencerMode = SequencerMode.MODE_4_STEP

        pulse1.reset()
        pulse2.reset()
//        Triangle.reset()
//        Noise.reset()
        DMC.reset()
    }

    fun getSample(): Float {
        val pulseSample = 0.00752f * (pulse1.output + pulse2.output)
        //val tndSample = 0.00851f * Triangle.output + 0.00494f * Noise.output + 0.00335f * DMC.output
        val tndSample = 0
        return pulseSample + tndSample
        //return if ((pulse1.dutyCycle shl pulse1.phase) and 0x80 > 0) -0.05f else 0.0f
    }
}