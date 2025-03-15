package com.onandor.nesemu.emulation.nes.mappers

import com.onandor.nesemu.emulation.nes.Cartridge
import com.onandor.nesemu.emulation.savestate.MapperState

// UxROM - https://www.nesdev.org/wiki/UxROM
class Mapper2(cartridge: Cartridge) : Mapper(cartridge) {

    private var bankSelect: Int = 0

    override fun readPrgRom(address: Int): Int {
        var eaddress = address - 0x8000
        return if (eaddress < 0x4000) {
            // Reading switchable bank
            cartridge.prgRom[bankSelect * 0x4000 + eaddress]
        } else {
            // Reading last bank (fixed)
            val bankAddress = (cartridge.prgRomBanks - 1) * 0x4000
            cartridge.prgRom[bankAddress + (eaddress and 0x3FFF)]
        }
    }

    override fun writePrgRom(address: Int, value: Int) {
        bankSelect = value and 0xFF
    }

    override fun readChrRom(address: Int): Int {
        return if (cartridge.chrRam != null) {
            cartridge.chrRam!![address]
        } else {
            cartridge.chrRom[address]
        }
    }

    override fun writeChrRom(address: Int, value: Int) {
        if (cartridge.chrRam != null) {
            cartridge.chrRam!![address] = value
        }
    }

    override fun saveState(): MapperState {
        return MapperState(
            bankSelect = bankSelect
        )
    }

    override fun loadState(state: MapperState) {
        bankSelect = state.bankSelect!!
    }
}