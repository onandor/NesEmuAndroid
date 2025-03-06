package com.onandor.nesemu.emulation

interface EmulationListener {
    fun onFrameReady(
        frame: IntArray,
        patternTable: IntArray,
        nametable: IntArray,
        colorPalettes: Array<IntArray>
    )
}