package com.onandor.nesemu.nes.mappers

import com.onandor.nesemu.nes.Cartridge

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
            val bankAddress = (cartridge.header.numPrgBank - 1) * 0x4000
            cartridge.prgRom[bankAddress + (eaddress and 0x3FFF)]
        }
    }

    override fun writePrgRom(address: Int, value: Int) {
        bankSelect = value and 0xFF
    }

    override fun readChrRom(address: Int): Int {
        return cartridge.chrRom[address]
    }

    override fun writeChrRom(address: Int, value: Int) {
        cartridge.chrRom[address] = value
    }
}