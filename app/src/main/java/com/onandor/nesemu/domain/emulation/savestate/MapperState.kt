package com.onandor.nesemu.domain.emulation.savestate

import com.onandor.nesemu.domain.emulation.nes.mappers.Mapper1
import kotlinx.serialization.Serializable

@Serializable
data class MapperState(
    val mapper1State: Mapper1State? = null,
    val mapper2State: Mapper2State? = null,
    val mapper3State: Mapper3State? = null
) : SaveState()

@Serializable
data class Mapper1State(
    val shifter: Int,
    val shiftCount: Int,
    val prgBankSize: Mapper1.PrgBankSize,
    val chrBankSize: Mapper1.ChrBankSize,
    val prgBankSwitchMode: Mapper1.PrgBankSwitchMode,
    val prgRomBank: Int,
    val chrRomBank4k0: Int,
    val chrRomBank4k1: Int,
    val chrRomBank8k: Int,
    val isPrgRamEnabled: Boolean
)

@Serializable
data class Mapper2State(
    val prgRomBank: Int
)

@Serializable
data class Mapper3State(
    val chrRomBank: Int
)