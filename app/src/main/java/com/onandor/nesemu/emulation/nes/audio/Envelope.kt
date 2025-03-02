package com.onandor.nesemu.emulation.nes.audio

class Envelope : Clockable {

    var isStarted: Boolean = false
    var isLooping: Boolean = false
    var isConstant: Boolean = false
    var volume: Int = 0

    val divider = Divider {
        if (volume > 0) {
            volume -= 1
        } else if (isLooping) {
            volume = 15
        }
    }

    fun getOutput(): Int = if (isConstant) divider.period else volume

    override fun clock() {
        if (!isStarted) {
            divider.clock()
        } else {
            isStarted = false
            volume = 15
            divider.reload()
        }
    }

    override fun reset() {
        isStarted = false
        isLooping = false
        isConstant = false
        volume = 0

        divider.reset()
    }
}