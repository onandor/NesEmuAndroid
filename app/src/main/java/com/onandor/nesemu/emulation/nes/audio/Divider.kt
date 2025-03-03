package com.onandor.nesemu.emulation.nes.audio

// https://www.nesdev.org/wiki/APU#Glossary
class Divider(private val outClock: () -> Unit) : Clockable {
    var counter: Int = 0
    var period: Int = 0

    override fun reset() {
        counter = 0
        period = 0
    }

    fun reload() {
        counter = period + 1
    }

    override fun clock() {
        if (counter == 0) {
            if (period != 0) {
                reload()
                outClock()
            }
        } else {
            counter -= 1
        }
    }
}