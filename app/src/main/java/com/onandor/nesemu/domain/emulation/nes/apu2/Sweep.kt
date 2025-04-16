package com.onandor.nesemu.domain.emulation.nes.apu2

// https://www.nesdev.org/wiki/APU_Sweep
// The sweep unit can periodically increase or decrease the corresponding pulse channel's period, effectively changing
// its pitch.

class Sweep(private val pulse: PulseChannel) {

    var divider: Int = 0
    var dividerPeriod: Int = 0
    var targetPulsePeriod: Int = 0
    var shiftCount: Int = 0
    var reload: Boolean = false
    var negate: Boolean = false
    var enabled: Boolean = false

    fun clock() {
        if (divider == 0 && enabled && shiftCount > 0 && !isMuting()) {
            pulse.timerPeriod = targetPulsePeriod
            updateTargetPeriod()
        }
        if (divider == 0 || reload) {
            divider = dividerPeriod
            reload = false
        } else {
            divider -= 1
        }
    }

    fun updateTargetPeriod() {
        var change = pulse.timerPeriod ushr shiftCount
        if (negate) {
            change *= -1
            if (pulse.channelNumber == PulseChannel.CHANNEL_1) {
                change -= 1
            }
        }

        targetPulsePeriod = (pulse.timerPeriod + change).coerceAtLeast(0)
    }

    fun load(value: Int) {
        enabled = value and 0x80 != 0
        dividerPeriod = (value and 0x70) ushr 4
        negate = value and 0x08 != 0
        shiftCount = value and 0x07
        reload = true
        updateTargetPeriod()
    }

    fun isMuting() = pulse.timerPeriod < 8 || targetPulsePeriod > 0x07FF

    fun reset() {
        divider = 0
        dividerPeriod = 0
        shiftCount = 0
        reload = false
        negate = false
        enabled = false
    }
}