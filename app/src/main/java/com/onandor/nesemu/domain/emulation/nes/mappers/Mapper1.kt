package com.onandor.nesemu.domain.emulation.nes.mappers

import com.onandor.nesemu.domain.emulation.nes.Cartridge
import com.onandor.nesemu.domain.emulation.nes.Mirroring
import com.onandor.nesemu.domain.emulation.savestate.Mapper1State
import com.onandor.nesemu.domain.emulation.savestate.MapperState

// MMC1 - https://www.nesdev.org/wiki/MMC1
// http://kevtris.org/mappers/mmc1/index.html
class Mapper1(cartridge: Cartridge) : Mapper(cartridge) {

    enum class PrgBankSize {
        Single32K,
        Double16K
    }

    enum class ChrBankSize {
        Single8K,
        Double4K
    }

    enum class PrgBankSwitchMode {
        Last16KFixed,
        First16KFixed
    }

    private var shifter: Int = 0
    private var shiftCount: Int = 0

    private var prgBankSize = PrgBankSize.Double16K
    private var chrBankSize = ChrBankSize.Single8K
    private var prgBankSwitchMode = PrgBankSwitchMode.Last16KFixed
    private var prgRomBank: Int = 0
    private var chrRomBank4k0: Int = 0
    private var chrRomBank4k1: Int = 0
    private var chrRomBank8k: Int = 0
    private var isPrgRamEnabled: Boolean = false

    override fun readPrgRom(address: Int): Int {
        val eaddress = address - 0x8000
        return if (prgBankSize == PrgBankSize.Single32K) {
            // Whole 32K address space is switched at once
            cartridge.prgRom[prgRomBank * 0x8000 + eaddress]
        } else {
            if (prgBankSwitchMode == PrgBankSwitchMode.Last16KFixed) {
                // The upper 16K of the 32K address space is fixed to the last bank of the ROM
                return if (eaddress < 0x4000) {
                    cartridge.prgRom[prgRomBank * 0x4000 + eaddress]
                } else {
                    val bankAddress = (cartridge.prgRomBanks - 1) * 0x4000
                    cartridge.prgRom[bankAddress + (eaddress and 0x3FFF)]
                }
            } else {
                // The lower 16K of the 32K address space if fixed to the first bank of the ROM
                return if (eaddress < 0x4000) {
                    cartridge.prgRom[eaddress]
                } else {
                    cartridge.prgRom[prgRomBank * 0x4000 + (eaddress and 0x3FFF)]
                }
            }
        }
    }

    override fun writePrgRom(address: Int, value: Int) {
        if (value and 0x80 > 0) {
            // Writing a value with bit 7 set resets the shifter
            shifter = 0
            shiftCount = 0
            return
        }

        // Copy the lowest bit of the value into the highest bit of the shifter
        shifter = (shifter ushr 1) or ((value and 0x01) shl 4)
        shiftCount += 1

        if (shiftCount == 5) {
            // Final write sets the appropriate register and resets the shifter
            when (address) {
                in 0x8000 .. 0x9FFF -> setControlRegister(shifter)
                in 0xA000 .. 0xBFFF -> setChrBank0Register(shifter)
                in 0xC000 .. 0xDFFF -> setChrBank1Register(shifter)
                in 0xE000 .. 0xFFFF -> setPrgBankRegister(shifter)
            }
            shifter = 0
            shiftCount = 0
        }
    }

    override fun readChrRom(address: Int): Int {
        return if (chrBankSize == ChrBankSize.Double4K) {
            // 8K address space is divided up and switched separately by two 4K banks
            val bank = if (address < 0x1000) chrRomBank4k0 else chrRomBank4k1
            val eaddress = address and 0x0FFF
            if (cartridge.chrRomBanks == 0) {
                cartridge.chrRam!![bank * 0x1000 + eaddress]
            } else {
                cartridge.chrRom[bank * 0x1000 + eaddress]
            }
        } else {
            // Whole 8K address space is switched at once
            if (cartridge.chrRomBanks == 0) {
                cartridge.chrRam!![chrRomBank8k * 0x2000 + address]
            } else {
                cartridge.chrRom[chrRomBank8k * 0x2000 + address]
            }
        }
    }

    override fun writeChrRom(address: Int, value: Int) {
        if (chrBankSize == ChrBankSize.Double4K) {
            val bank = if (address < 0x1000) chrRomBank4k0 else chrRomBank4k1
            val eaddress = address and 0x0FFF
            if (cartridge.chrRomBanks == 0) {
                cartridge.chrRam!![bank * 0x1000 + eaddress] = value
            } else {
                cartridge.chrRom[bank * 0x1000 + eaddress] = value
            }
        } else {
            if (cartridge.chrRomBanks == 0) {
                cartridge.chrRam!![chrRomBank8k * 0x2000 + address] = value
            } else {
                cartridge.chrRom[chrRomBank8k * 0x2000 + address] = value
            }
        }
    }

    override fun readPrgRam(address: Int): Int {
        if (!isPrgRamEnabled) {
            return OPEN_BUS
        }
        return cartridge.prgRam?.get(address and 0x1FFF) ?: OPEN_BUS
    }

    override fun writePrgRam(address: Int, value: Int) {
        if (isPrgRamEnabled && cartridge.prgRam != null) {
            cartridge.prgRam!![address and 0x1FFF] = value
        }
    }

    override fun reset() {
        shifter = 0b10000
        shiftCount = 0
        prgBankSize = PrgBankSize.Double16K
        chrBankSize = ChrBankSize.Single8K
        prgBankSwitchMode = PrgBankSwitchMode.Last16KFixed
        prgRomBank = 0
        chrRomBank4k0 = 0
        chrRomBank4k1 = 0
        chrRomBank8k = 0
        isPrgRamEnabled = false
    }

    private fun setControlRegister(value: Int) {
        when (value and 0b11) {
            0b00 -> cartridge.mirroring = Mirroring.SingleScreenLowerBank
            0b01 -> cartridge.mirroring = Mirroring.SingleScreenUpperBank
            0b10 -> cartridge.mirroring = Mirroring.Vertical
            0b11 -> cartridge.mirroring = Mirroring.Horizontal
        }

        prgBankSwitchMode = if (value and 0b100 > 1) {
            PrgBankSwitchMode.Last16KFixed
        } else {
            PrgBankSwitchMode.First16KFixed
        }
        prgBankSize = if (value and 0b1000 > 0) PrgBankSize.Double16K else PrgBankSize.Single32K
        chrBankSize = if (value and 0b10000 > 0) ChrBankSize.Double4K else ChrBankSize.Single8K
    }

    private fun setChrBank0Register(value: Int) {
        if (chrBankSize == ChrBankSize.Single8K) {
            chrRomBank8k = value and 0b11110
        } else {
            chrRomBank4k0 = value and 0b11111
        }
    }

    private fun setChrBank1Register(value: Int) {
        if (chrBankSize == ChrBankSize.Double4K) {
            chrRomBank4k1 = value and 0x1F
        }
    }

    private fun setPrgBankRegister(value: Int) {
        prgRomBank = if (prgBankSize == PrgBankSize.Single32K) {
            value and 0b1110
        } else {
            value and 0b1111
        }
        isPrgRamEnabled = (value and 0b10000) == 0
    }

    override fun captureState(): MapperState {
        val state = Mapper1State(
            shifter = shifter,
            shiftCount = shiftCount,
            prgBankSize = prgBankSize,
            chrBankSize = chrBankSize,
            prgBankSwitchMode = prgBankSwitchMode,
            prgRomBank = prgRomBank,
            chrRomBank4k0 = chrRomBank4k0,
            chrRomBank4k1 = chrRomBank4k1,
            chrRomBank8k = chrRomBank8k,
            isPrgRamEnabled = isPrgRamEnabled
        )
        return MapperState(mapper1State = state)
    }

    override fun loadState(state: MapperState) {
        val state = state.mapper1State!!
        shifter = state.shifter
        shiftCount = state.shiftCount
        prgBankSize = state.prgBankSize
        chrBankSize = state.chrBankSize
        prgBankSwitchMode = state.prgBankSwitchMode
        prgRomBank = state.prgRomBank
        chrRomBank4k0 = state.chrRomBank4k0
        chrRomBank4k1 = state.chrRomBank4k1
        chrRomBank8k = state.chrRomBank8k
        isPrgRamEnabled = state.isPrgRamEnabled
    }
}