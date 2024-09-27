package com.onandor.nesemu.nes.mappers

import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.InvalidOperationException

// NROM - https://www.nesdev.org/wiki/NROM
class Mapper0(cartridge: Cartridge) : Mapper(cartridge) {

    companion object {
        private const val TAG = "Mapper0"
    }

    override fun readPrgRom(address: Int): Int {
        // Shifting to offset 0 and mirroring
        val eaddress = (address - 0x8000) and 0x3FFF
        return cartridge.prgRom[eaddress]
    }

    override fun writePrgRom(address: Int, value: Int) {
        throw InvalidOperationException(TAG, "Invalid PRG ROM write at $address (value: $value)")
    }
}