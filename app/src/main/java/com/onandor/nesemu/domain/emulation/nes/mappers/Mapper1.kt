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

    private var shifter: Int = 0b10000

    private var prgBankSize = PrgBankSize.Double16K
    private var chrBankSize = ChrBankSize.Single8K
    private var prgBankSwitchMode = PrgBankSwitchMode.Last16KFixed
    private var prgRomBank: Int = 0
    private var chrRomBank0: Int = 0
    private var chrRomBank1: Int = 0
    private var isPrgRamEnabled: Boolean = false
    private val usesChrRam: Boolean = cartridge.chrRam != null

    private val chrMemory: IntArray
    private val chrMemoryBanks: Int

    init {
        if (cartridge.chrRam != null) {
            chrMemory = cartridge.chrRam!!
            chrMemoryBanks = cartridge.chrRam!!.size / 0x2000
        } else {
            chrMemory = cartridge.chrRom
            chrMemoryBanks = cartridge.chrRomBanks
        }
    }

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
            shifter = 0b10000
            return
        }

        val finalWrite = shifter or 1 == 1

        // Copy the new bit into the shifter
        shifter = (shifter or (value shl 4)) ushr 1

        if (finalWrite) {
            // Final write sets the appropriate register and resets the shifter
            when (address) {
                in 0x8000 .. 0x9FFF -> setControlRegister(shifter)
                in 0xA000 .. 0xBFFF -> setChrBank1Register(shifter)
                in 0xC000 .. 0xDFFF -> setChrBank2Register(shifter)
                in 0xE000 .. 0xFFFF -> setPrgBankRegister(shifter)
            }
            shifter = 0b10000
        }
    }

    override fun readChrRom(address: Int): Int {
        return if (chrBankSize == ChrBankSize.Double4K) {
            // 8K address space is divided up and switched separately by two 4K banks
            val bank = if (address < 0x1000) chrRomBank0 else chrRomBank1
            val eaddress = address and 0x0FFF
            chrMemory[bank * 0x2000 + eaddress]
        } else {
            // Whole 8K address space is switched at once
            chrMemory[chrRomBank0 * 0x2000 + address]
        }
    }

    override fun readPrgRam(address: Int): Int {
        if (!isPrgRamEnabled) {
            return OPEN_BUS
        }
        return cartridge.prgRam?.get(address) ?: OPEN_BUS
    }

    override fun writePrgRam(address: Int, value: Int) {
        if (isPrgRamEnabled && cartridge.prgRam != null) {
            cartridge.prgRam!![address] = value
        }
    }

    override fun reset() {
        shifter = 0b10000
        prgBankSize = PrgBankSize.Double16K
        chrBankSize = ChrBankSize.Single8K
        prgBankSwitchMode = PrgBankSwitchMode.Last16KFixed
        prgRomBank = 0
        chrRomBank0 = 0
        chrRomBank1 = 0
        isPrgRamEnabled = false
    }

    private fun setControlRegister(value: Int) {
        when (value and 0b11) {
            0b00 -> cartridge.mirroring = Mirroring.SingleScreen // TODO: nametable select - http://kevtris.org/mappers/mmc1/index.html
            0b01 -> cartridge.mirroring = Mirroring.SingleScreen // TODO: nametable select
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

    private fun setChrBank1Register(value: Int) {
        chrRomBank0 = if (chrBankSize == ChrBankSize.Single8K) {
            // On real hardware, the address lines used for selecting the banks are only wide enough
            // to index the available number of banks => discard the unnecessary high bits
            (value and 0b11110) and (chrMemoryBanks - 1)
        } else {
            (value and 0b11111) and (chrMemoryBanks - 1)
        }
    }

    private fun setChrBank2Register(value: Int) {
        if (chrBankSize == ChrBankSize.Double4K) {
            chrRomBank1 = value and 0x1F
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

    override fun createSaveState(): MapperState {
        val state = Mapper1State(
            shifter = shifter,
            prgBankSize = prgBankSize,
            chrBankSize = chrBankSize,
            prgBankSwitchMode = prgBankSwitchMode,
            prgRomBank = prgRomBank,
            chrRomBank0 = chrRomBank0,
            chrRomBank1 = chrRomBank1,
            isPrgRamEnabled = isPrgRamEnabled
        )
        return MapperState(mapper1State = state)
    }

    override fun loadState(state: MapperState) {
        val _state = state.mapper1State!!
        shifter = _state.shifter
        prgBankSize = _state.prgBankSize
        chrBankSize = _state.chrBankSize
        prgBankSwitchMode = _state.prgBankSwitchMode
        prgRomBank = _state.prgRomBank
        chrRomBank0 = _state.chrRomBank0
        chrRomBank1 = _state.chrRomBank1
        isPrgRamEnabled = _state.isPrgRamEnabled
    }
}