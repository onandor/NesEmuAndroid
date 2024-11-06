package com.onandor.nesemu.nes.mappers

import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.Mirroring

abstract class Mapper(open val cartridge: Cartridge) {

    abstract fun readPrgRom(address: Int): Int
    abstract fun writePrgRom(address: Int, value: Int)
    abstract fun readChrRom(address: Int): Int
    abstract fun writeChrRom(address: Int, value: Int)
    abstract fun readUnmappedRange(address: Int): Int
    abstract fun writeUnmappedRange(address: Int, value: Int)
    abstract fun readRam(address: Int): Int
    abstract fun writeRam(address: Int, value: Int)

    private val nametableOffsetMap: Map<Mirroring, Map<Int, Int>> = mapOf(
        Mirroring.HORIZONTAL to mapOf(
            0 to 0x000,
            1 to 0x400,
            2 to 0x400,
            3 to 0x800
        ),
        Mirroring.VERTICAL to mapOf(
            0 to 0x000,
            1 to 0x000,
            2 to 0x800,
            3 to 0x800
        ),
        Mirroring.SINGLE_SCREEN to mapOf(
            0 to 0x000,
            1 to 0x000,
            2 to 0x000,
            3 to 0x000
        ),
        Mirroring.FOUR_SCREEN to mapOf(
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
}