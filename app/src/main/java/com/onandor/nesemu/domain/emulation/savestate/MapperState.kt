package com.onandor.nesemu.domain.emulation.savestate

import kotlinx.serialization.Serializable

@Serializable
data class MapperState(
    val bankSelect: Int? = null
) : SaveState()
