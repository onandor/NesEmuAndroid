package com.onandor.nesemu.domain.emulation.nes.apu2

// https://www.nesdev.org/wiki/APU_Triangle
// The triangle channel produces a triangle waveform. It outputs a volume in the range of 0 and 15, chosen from 32
// possible values depending on the sequencer.

class TriangleChannel {

    // The sequencer holds a value between 0 and 32, indexing into the SEQUENCE_LOOKUP table, which holds the volume
    // levels that the channel outputs.
    private var sequencer: Int = 0

    // The timer has an 11 bit long period, which is decreased every CPU (!) cycle
    // When it reaches zero, it clocks the sequencer, and is automatically reloaded with the value stored in timerPeriod
    private var timer: Int = 0
    private var timerPeriod: Int = 0

    // High precision linear counter for duration
    private var linearCounter: Int = 0
    private var linearCounterPeriod: Int = 0
    private var reloadLinearCounter: Boolean = false

    // Control bit for the length counter and the linear counter
    private var controlFlag: Boolean = false

    private val lengthCounter = LengthCounter()

    fun clockTimer() {
        timer -= 1
        if (timer == -1) {
            timer = timerPeriod
            if (lengthCounter.length > 0 && linearCounter > 0) {
                // The triangle channel doesn't become silenced the same way the pulse channels do. When either of the
                // counters reaches zero, it halts (stops clocking the sequencer) and continues to output its last
                // value rather than 0.
                sequencer = (sequencer + 1) % 32
            }
        }
    }

    fun clockLinearCounter() {
        if (reloadLinearCounter) {
            linearCounter = linearCounterPeriod
        } else if (linearCounter > 0) {
            linearCounter -= 1
        }
        if (!controlFlag) {
            reloadLinearCounter = false
        }
    }

    fun clockLengthCounter() {
        lengthCounter.clock()
    }

    fun reset() {
        sequencer = 0
        timer = 0
        timerPeriod = 0
        linearCounter = 0
        linearCounterPeriod = 0
        reloadLinearCounter = false
        controlFlag = false
        lengthCounter.reset()
    }

    // 0x4008
    fun writeLinearCounter(value: Int) {
        lengthCounter.enabled = (value and 0x80) == 0
        controlFlag = (value and 0x80) != 0
        linearCounterPeriod = value and 0x7F
    }

    // 0x400A
    fun writeTimer(value: Int) {
        timerPeriod = (timerPeriod and 0x700) or value
    }

    // 0x400B
    fun writeLengthCounter(value: Int) {
        timerPeriod = ((value and 0x07) shl 8) or (timerPeriod and 0x00FF)
        lengthCounter.load(value ushr 3)
        reloadLinearCounter = true
    }

    fun setEnabled(enabled: Boolean) {
        lengthCounter.enabled = enabled
        if (!enabled) {
            lengthCounter.length = 0
        }
    }

    fun getOutput(): Int {
        return SEQUENCE_LOOKUP[sequencer]
    }

    companion object {
        private val SEQUENCE_LOOKUP: Array<Int> = arrayOf(
            15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,
            0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15
        )
    }
}