package com.onandor.nesemu.emulation.nes.audio

interface Clockable {
    fun clock()
    fun reset()
}