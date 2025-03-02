package com.onandor.nesemu.emulation.nes.audio

class Sweep(private val pulse: PulseChannel, private val channel: Int) : Clockable {

    var isEnabled: Boolean = false
    var isNegated: Boolean = false
    var reload: Boolean = false
    var shiftCount: Int = 0

    val divider = Divider {
//        var change = pulseDivider.period ushr shiftCount
//        if (isNegated) {
//            change *= -1
//        }
//
    }

    override fun clock() {
        if (divider.counter == 0 && isEnabled && shiftCount > 0) { // TODO: not muted

        }
        if (divider.counter == 0 || reload) {
            divider.counter = divider.period
            reload = false
        } else {
            divider.clock()
        }
    }

    override fun reset() {
        isEnabled = false
        isNegated = false
        reload = false
        shiftCount = 0

        divider.reset()
    }
}