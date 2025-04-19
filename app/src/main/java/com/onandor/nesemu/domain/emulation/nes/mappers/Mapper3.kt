package com.onandor.nesemu.domain.emulation.nes.mappers

import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.savestate.Mapper3State
import com.onandor.nesemu.domain.emulation.savestate.MapperState

// CNROM - https://www.nesdev.org/wiki/INES_Mapper_003
class Mapper3(cartridge: Cartridge) : Mapper(cartridge) {

    private var chrRomBank: Int = 0

    override fun readPrgRom(address: Int): Int {
        var romAddress = address - 0x8000
        if (cartridge.prgRom.size == 0x4000) {
            romAddress = romAddress and 0x3FFF
        }
        return cartridge.prgRom[romAddress]
    }

    override fun writePrgRom(address: Int, value: Int) {
        chrRomBank = value and 0xFF
    }

    override fun readChrRom(address: Int): Int {
        return cartridge.chrRom[chrRomBank * 0x2000 + address]
    }

    override fun reset() {
        chrRomBank = 0
    }

    override fun createSaveState(): MapperState {
        val state = Mapper3State(chrRomBank = chrRomBank)
        return MapperState(mapper3State = state)
    }

    override fun loadState(state: MapperState) {
        chrRomBank = state.mapper3State!!.chrRomBank
    }
}