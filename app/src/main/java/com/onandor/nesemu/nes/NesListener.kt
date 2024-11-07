package com.onandor.nesemu.nes

interface NesListener {

    fun onFrameReady()
    fun onReadButtons()
}