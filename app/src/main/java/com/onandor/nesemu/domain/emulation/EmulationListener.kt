package com.onandor.nesemu.domain.emulation

import com.onandor.nesemu.domain.emulation.nes.Nes

interface EmulationListener {
    fun onFrameReady(frame: Nes.Frame)
}