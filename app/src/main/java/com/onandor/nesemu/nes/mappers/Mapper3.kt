package com.onandor.nesemu.nes.mappers

import android.util.Log
import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.toHexString

class Mapper3(cartridge: Cartridge) : Mapper(cartridge) {

    companion object {
        private const val TAG = "Mapper3"
    }

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

    override fun writeChrRom(address: Int, value: Int) {
        Log.w(TAG, "CPU attempting to write CHR ROM at $${address.toHexString(4)}" +
                " (value: $${value.toHexString(2)})")
    }

    override fun readUnmappedRange(address: Int): Int {
        Log.i(TAG, "CPU reading unmapped address $${address.toHexString(4)}")
        return -1
    }

    override fun writeUnmappedRange(address: Int, value: Int) {
        Log.i(TAG, "CPU writing unmapped address $${address.toHexString(4)}" +
                " (value: $${value.toHexString(2)})")
    }

    override fun readRam(address: Int): Int {
        Log.i(TAG, "CPU reading unmapped address $${address.toHexString(4)}")
        return -1
    }

    override fun writeRam(address: Int, value: Int) {
        Log.i(TAG, "CPU writing unmapped address $${address.toHexString(4)}" +
                " (value: $${value.toHexString(2)})")
    }
}