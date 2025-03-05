package com.onandor.nesemu.emulation.nes.audio

class Sweep(private val pulse: PulseChannel, private val channel: Int) : Clockable {

    var isEnabled: Boolean = false
    var isNegated: Boolean = false
    var reload: Boolean = false
    var shiftCount: Int = 0
    var targetPeriod: Int = 0

    var divider = Divider {
        var change = pulse.divider.period ushr shiftCount
        if (isNegated) {
            change *= -1
            if (channel == Apu.PULSE_CHANNEL_1) {
                change -= 1
            }
        }
        targetPeriod = (pulse.divider.period + change).coerceAtLeast(0)
    }

    override fun clock() {
        if (divider.counter == 0 && isEnabled && shiftCount > 0) {
            if (!muting()) {
                pulse.divider.period = targetPeriod
            }
        }
        if (divider.counter == 0 || reload) {
            divider.reload()
            reload = false
        } else {
            divider.clock()
        }
    }

    fun muting() = targetPeriod > 0x7FF || pulse.divider.period < 8

    override fun reset() {
        isEnabled = false
        isNegated = false
        reload = false
        shiftCount = 0

        divider.reset()
    }
}