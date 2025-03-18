package com.onandor.nesemu.emulation.savestate

import kotlinx.serialization.Serializable

@Serializable
data class MapperState(
    val bankSelect: Int? = null
) : SaveState()
