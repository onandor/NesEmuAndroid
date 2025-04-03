package com.onandor.nesemu.domain.emulation.nes.apu

interface Clockable {
    fun clock()
    fun reset()
}