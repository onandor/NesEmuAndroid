package com.onandor.nesemu.domain.emulation.nes.mappers

import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.savestate.MapperState

// CNROM - https://www.nesdev.org/wiki/INES_Mapper_003
class Mapper3(cartridge: Cartridge) : Mapper(cartridge) {

    private var bankSelect: Int = 0

    override fun readPrgRom(address: Int): Int {
        var eaddress = address - 0x8000
        if (cartridge.prgRom.size == 0x4000) {
            eaddress = eaddress and 0x3FFF
        }
        return cartridge.prgRom[eaddress]
    }

    override fun writePrgRom(address: Int, value: Int) {
        bankSelect = value and 0xFF
    }

    override fun readChrRom(address: Int): Int {
        return cartridge.chrRom[bankSelect * 0x2000 + address]
    }

    override fun createSaveState(): MapperState {
        return MapperState(
            bankSelect = bankSelect
        )
    }

    override fun loadState(state: MapperState) {
        bankSelect = state.bankSelect!!
    }
}