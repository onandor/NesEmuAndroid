package com.onandor.nesemu.emulation.nes.apu

interface Clockable {
    fun clock()
    fun reset()
}