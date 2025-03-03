package com.onandor.nesemu.emulation.nes.audio

class TriangleChannel : Clockable {

    var length: Int = 0
    var control: Boolean = false
    var counter: Int = 0
    var reloadValue: Int = 0
    var reloadCounter: Boolean = false
    var phase: Int = 0

    val divider = Divider {
        if (counter > 0 && length > 0) {
            phase = (phase + 1) % 32
        }
    }

    override fun clock() {
        if (!control && length > 0) {
            length -= 1
        }
    }

    fun clockCounter() {
        if (reloadCounter) {
            counter = reloadValue
        } else if (counter > 0) {
            counter -= 1
        }

        if (!control) {
            reloadCounter = false
        }
    }

    override fun reset() {
        length = 0
        control = false
        counter = 0
        reloadValue = 0
        reloadCounter = false
        phase = 0

        divider.reset()
    }

    fun setControl(value: Int) {
        control = value and 0x80 > 0
        reloadValue = value and 0x7F
    }

    fun setDividerLow(value: Int) {
        divider.period = (divider.period and 0x700) or (value and 0xFF)
    }

    fun setDividerHigh(value: Int) {
        divider.period = ((value and 0b111) shl 8) or (divider.period and 0xFF)
        divider.reload()
        reloadCounter = true
        length = Apu2.LENGTH_COUNTER_LOOKUP[(value and 0xF8) ushr 3]
    }

    fun getOutput(): Int {
        return if (divider.period < 2 || length == 0 || counter == 0) {
            0
        } else {
            SEQUENCE[phase]
        }
    }

    companion object {
        private val SEQUENCE: IntArray = intArrayOf(
            15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,
             0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15
        )
    }
}