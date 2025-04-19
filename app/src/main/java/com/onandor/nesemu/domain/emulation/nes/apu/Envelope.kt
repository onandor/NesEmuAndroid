package com.onandor.nesemu.domain.emulation.nes.apu

// https://www.nesdev.org/wiki/APU_Envelope
// The envelope controls the main volume of the pulse and noise channels. It is clocked on every quarter frame.
// It outputs a volume between 0 and 15, which can either be constant, or decaying depending on the configuration
// set in the control register. The decaying of the volume is done by continuously clocking the envelope.
// When it outputs a decaying value, depending on the loop variable, it can either reset the volume after it reaches 0
// or remain muted until it is manually restarted.

// https://www.nesdev.org/wiki/APU_Envelope
class Envelope {

    var start: Boolean = false
    var divider: Int = 0
    var volume: Int = 0
    var decay: Int = 0
    var constant: Boolean = false
    var loop: Boolean = false

    fun clock() {
        if (!start) {
            divider -= 1
            if (divider == -1) {
                divider = volume
                if (decay > 0) {
                    decay -= 1
                } else if (loop) {
                    decay = 15
                }
            }
        } else {
            start = false
            decay = 15
            divider = volume
        }
    }

    fun getOutput(): Int = if (constant) volume else decay

    fun reset() {
        start = false
        divider = 0
        volume = 0
        decay = 0
        constant = false
        loop = false
    }
}