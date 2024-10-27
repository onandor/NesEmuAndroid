package com.onandor.nesemu.nes.mappers

import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.InvalidOperationException

// NROM - https://www.nesdev.org/wiki/NROM
class Mapper0(cartridge: Cartridge) : Mapper(cartridge) {

    companion object {
        private const val TAG = "Mapper0"
    }

    override fun readPrgRom(address: Int): Int {
        // Shifting to offset 0 and mirroring if only 1 bank is present
        var eaddress = address - 0x8000
        if (cartridge.prgRom.size == 0x4000) {
            eaddress = eaddress and 0x3FFF
        }
        return cartridge.prgRom[eaddress]
    }

    override fun writePrgRom(address: Int, value: Int) {
        throw InvalidOperationException(TAG, "Forbidden PRG ROM write at $address (value: $value)")
    }

    override fun readChrRom(address: Int): Int {
        return cartridge.chrRom[address]
    }

    override fun writeChrRom(address: Int, value: Int) {
        throw InvalidOperationException(TAG, "Forbidden CHR ROM write at $address (value: $value)")
    }
}