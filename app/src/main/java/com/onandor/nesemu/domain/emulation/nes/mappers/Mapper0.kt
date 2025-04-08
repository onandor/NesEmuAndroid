package com.onandor.nesemu.domain.emulation.nes.mappers

import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.savestate.MapperState

// NROM - https://www.nesdev.org/wiki/NROM
class Mapper0(cartridge: Cartridge) : Mapper(cartridge) {

    override fun readPrgRom(address: Int): Int {
        // Shifting to offset 0 and mirroring if only 1 bank is present
        var eaddress = address - 0x8000
        if (cartridge.prgRom.size == 0x4000) {
            eaddress = eaddress and 0x3FFF
        }
        return cartridge.prgRom[eaddress]
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

    override fun readPrgRam(address: Int): Int {
        cartridge.prgRam?.let {
            return it[address - 0x6000]
        }
        return OPEN_BUS
    }

    override fun writePrgRam(address: Int, value: Int) {
        cartridge.prgRam?.let {
            it[address - 0x6000] = value
        }
    }

    override fun createSaveState(): MapperState {
        return MapperState()
    }

    override fun loadState(state: MapperState) {}
}