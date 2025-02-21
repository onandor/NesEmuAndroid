package com.onandor.nesemu.emulation.nes

interface NesListener {

    fun onFrameReady()
    fun onPollController1Buttons(): Int? { return null }
    fun onPollController2Buttons(): Int? { return null }
}