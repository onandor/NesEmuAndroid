package com.onandor.nesemu.domain.emulation.nes.apu2

// https://www.nesdev.org/wiki/APU_Noise
// The noise channel generates pseudo-random noise.

class NoiseChannel {

    // The timer has 16 different periods (the channel has 16 different frequencies), which are chosen from the
    // TIMER_PERIOD_LOOKUP table. The timer clocks the linear feedback shift register, which results in a pseudo-random
    // bit sequence, which is used to silence the channel.
    private var timer: Int = 0
    private var timerPeriod: Int = 0

    private var shifter: Int = 1
    private var mode: Boolean = false

    private val envelope = Envelope()
    private val lengthCounter = LengthCounter()

    fun clockTimer() {
        timer -= 1
        if (timer == -1) {
            timer = timerPeriod

            val shift = if (mode) 6 else 1
            val feedback = (shifter and 0x01) xor ((shifter ushr shift) and 0x01)
            shifter = (shifter ushr 1) or (feedback shl 14)
        }
    }

    fun clockEnvelope() {
        envelope.clock()
    }

    fun clockLengthCounter() {
        lengthCounter.clock()
    }

    fun reset() {
        timer = 0
        timerPeriod = 0
        shifter = 1
        mode = false
        envelope.reset()
        lengthCounter.reset()
    }

    // 0x400C
    fun writeControl(value: Int) {
        setEnabled((value and 0x20) == 0)
        envelope.loop = (value and 0x20) != 0
        envelope.constant = (value and 0x10) != 0
        envelope.volume = value and 0x0F
    }

    // 0x400E
    fun writeTimer(value: Int) {
        mode = value and 0x80 != 0
        timerPeriod = TIMER_PERIOD_LOOKUP[value and 0x0F]
    }

    // 0x400F
    fun writeLengthCounter(value: Int) {
        lengthCounter.load(value ushr 3)
        envelope.start = true
    }

    fun setEnabled(enabled: Boolean) {
        lengthCounter.enabled = enabled
        if (!enabled) {
            lengthCounter.length = 0
        }
    }

    fun getOutput(): Int {
        return if (shifter and 0x01 != 0 || lengthCounter.length == 0) 0 else envelope.getOutput()
    }

    companion object {
        private val TIMER_PERIOD_LOOKUP: Array<Int> = arrayOf(
            4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
        )
    }
}