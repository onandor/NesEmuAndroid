package com.onandor.nesemu.domain.emulation.nes.mappers

import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.savestate.Mapper2State
import com.onandor.nesemu.domain.emulation.savestate.MapperState

// UxROM - https://www.nesdev.org/wiki/UxROM
class Mapper2(cartridge: Cartridge) : Mapper(cartridge) {

    private var prgRomBank: Int = 0

    override fun readPrgRom(address: Int): Int {
        var eaddress = address - 0x8000
        return if (eaddress < 0x4000) {
            // Reading switchable bank
            cartridge.prgRom[prgRomBank * 0x4000 + eaddress]
        } else {
            // Reading last bank (fixed)
            val bankAddress = (cartridge.prgRomBanks - 1) * 0x4000
            cartridge.prgRom[bankAddress + (eaddress and 0x3FFF)]
        }
    }

    override fun writePrgRom(address: Int, value: Int) {
        prgRomBank = value and 0xFF
    }

    override fun readChrRom(address: Int): Int {
        return if (cartridge.chrRam != null) {
            cartridge.chrRam!![address]
        } else {
            cartridge.chrRom[address]
        }
    }

    override fun writeChrRom(address: Int, value: Int) {
        cartridge.chrRam?.let {
            it[address] = value
        }
    }

    override fun createSaveState(): MapperState {
        val state = Mapper2State(prgRomBank = prgRomBank)
        return MapperState(mapper2State = state)
    }

    override fun loadState(state: MapperState) {
        prgRomBank = state.mapper2State!!.prgRomBank
    }
}