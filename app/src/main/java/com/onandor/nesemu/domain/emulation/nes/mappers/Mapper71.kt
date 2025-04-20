package com.onandor.nesemu.domain.emulation.nes.mappers

import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.nes.Mirroring
import com.onandor.nesemu.domain.emulation.savestate.Mapper71State
import com.onandor.nesemu.domain.emulation.savestate.MapperState

// https://www.nesdev.org/wiki/INES_Mapper_071
// This mapper is similar to mapper 2
class Mapper71(cartridge: Cartridge) : Mapper(cartridge) {

    private var prgRomBank: Int = 0

    override fun readPrgRom(address: Int): Int {
        val romAddress = address - 0x8000
        return if (romAddress < 0x4000) {
            // Reading switchable bank
            cartridge.prgRom[prgRomBank * 0x4000 + romAddress]
        } else {
            val bankAddress = (cartridge.prgRomBanks - 1) * 0x4000
            cartridge.prgRom[bankAddress + (romAddress and 0x3FFF)]
        }
    }

    override fun writePrgRom(address: Int, value: Int) {
        when (address) {
            in 0x9000 .. 0x9FFF -> {
                // Fire Hawk: single screen mirroring nametable select
                cartridge.mirroring = if (value and 0x10 == 0) {
                    Mirroring.SingleScreenLowerBank
                } else {
                    Mirroring.SingleScreenUpperBank
                }
            }
            in 0xC000 .. 0xFFFF -> {
                // PRG ROM bank select
                // Some games might use more than the 2 lowest bits, more info on the wiki
                prgRomBank = value and 0x03
            }
        }
    }

//    override fun readPrgRam(address: Int): Int {
//        cartridge.prgRam?.let {
//            return it[address - 0x6000]
//        }
//        return OPEN_BUS
//    }
//
//    override fun writePrgRam(address: Int, value: Int) {
//        cartridge.prgRam?.let {
//            it[address - 0x6000] = value
//        }
//    }

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

    override fun reset() {
        prgRomBank = 0
    }

    override fun captureState(): MapperState {
        val state = Mapper71State(prgRomBank = prgRomBank)
        return MapperState(mapper71State = state)
    }

    override fun loadState(state: MapperState) {
        prgRomBank = state.mapper71State!!.prgRomBank
    }
}