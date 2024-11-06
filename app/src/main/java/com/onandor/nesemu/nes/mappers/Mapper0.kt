package com.onandor.nesemu.nes.mappers

import android.util.Log
import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.InvalidOperationException
import com.onandor.nesemu.nes.toHexString

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
        Log.w(TAG, "CPU attempting to write PRG ROM at $${address.toHexString(4)}" +
                " (value: $${value.toHexString(2)})")
    }

    override fun readChrRom(address: Int): Int {
        return cartridge.chrRom[address]
    }

    override fun writeChrRom(address: Int, value: Int) {
        Log.w(TAG, "CPU attempting to write CHR ROM at $${address.toHexString(4)}" +
                " (value: $${value.toHexString(2)})")
    }

    override fun readUnmappedRange(address: Int): Int {
        return -1
    }

    override fun writeUnmappedRange(address: Int, value: Int) {}

    override fun readRam(address: Int): Int {
        return -1
    }

    override fun writeRam(address: Int, value: Int) {}
}