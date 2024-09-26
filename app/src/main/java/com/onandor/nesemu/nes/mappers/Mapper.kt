package com.onandor.nesemu.nes.mappers

import com.onandor.nesemu.nes.Cartridge

abstract class Mapper(open val cartridge: Cartridge) {

    abstract fun readPrgRom(address: Int): Int
    abstract fun writePrgRom(address: Int, value: Int)
}