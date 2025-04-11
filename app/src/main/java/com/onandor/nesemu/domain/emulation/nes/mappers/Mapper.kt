package com.onandor.nesemu.domain.emulation.nes.mappers

import android.util.Log
import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.nes.Mirroring
import com.onandor.nesemu.domain.emulation.nes.toHexString
import com.onandor.nesemu.domain.emulation.savestate.MapperState
import com.onandor.nesemu.domain.emulation.savestate.Savable

abstract class Mapper(open val cartridge: Cartridge) : Savable<MapperState> {

    companion object {
        internal const val TAG = "Mapper"
        internal const val OPEN_BUS = -1
    }

    private val nametableOffsetMap: Map<Mirroring, Map<Int, Int>> = mapOf(
        Mirroring.Horizontal to mapOf(
            0 to 0x000,
            1 to 0x400,
            2 to 0x400,
            3 to 0x800
        ),
        Mirroring.Vertical to mapOf(
            0 to 0x000,
            1 to 0x000,
            2 to 0x800,
            3 to 0x800
        ),
        Mirroring.SingleScreen to mapOf(
            0 to 0x000,
            1 to 0x400,
            2 to 0x800,
            3 to 0xC00
        ),
        Mirroring.FourScreen to mapOf(
            0 to 0x000,
            1 to 0x000,
            2 to 0x000,
            3 to 0x000
        )
    )

    open fun mapNametableAddress(address: Int): Int {
        val vramAddress = address - 0x2000
        val nametableIdx = vramAddress / 0x400
        return vramAddress - nametableOffsetMap[cartridge.mirroring]!![nametableIdx]!!
    }

    abstract fun readPrgRom(address: Int): Int

    open fun writePrgRom(address: Int, value: Int) {
        Log.w(TAG, "CPU attempting to write PRG ROM at $${address.toHexString(4)}" +
                " (value: $${value.toHexString(2)})")
    }

    abstract fun readChrRom(address: Int): Int

    open fun writeChrRom(address: Int, value: Int) {
        Log.w(TAG, "CPU attempting to write CHR ROM at $${address.toHexString(4)}" +
                " (value: $${value.toHexString(2)})")
    }

    open fun readPrgRam(address: Int): Int {
        Log.i(TAG, "CPU reading unmapped address $${address.toHexString(4)}")
        return 0
    }

    open fun writePrgRam(address: Int, value: Int) {
        Log.i(TAG, "CPU writing unmapped address $${address.toHexString(4)}" +
                " (value: $${value.toHexString(2)})")
    }

    open fun readUnmappedRange(address: Int): Int {
        Log.i(TAG, "CPU reading unmapped address $${address.toHexString(4)}")
        return 0
    }

    open fun writeUnmappedRange(address: Int, value: Int) {
        Log.i(TAG, "CPU writing unmapped address $${address.toHexString(4)}" +
                " (value: $${value.toHexString(2)})")
    }

    open fun reset() {}
}