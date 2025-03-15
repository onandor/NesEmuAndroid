package com.onandor.nesemu.emulation.savestate

data class MapperState(
    val bankSelect: Int? = null
) : SaveState()
