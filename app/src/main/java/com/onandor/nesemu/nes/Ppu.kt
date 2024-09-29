package com.onandor.nesemu.nes

class Ppu(
    private val readMemory: (address: Int) -> Int,
    private val writeMemory: (address: Int, value: Int) -> Unit
) {

    companion object {
        private const val TAG = "Ppu"
    }

    // https://www.nesdev.org/wiki/PPU_registers
    private object Registers {
        const val PPUCTRL = 0x2000
        const val PPUMASK = 0x2001
        const val PPUSTATUS = 0x2002
        const val OAMADDR = 0x2003
        const val OAMDATA = 0x2004
        const val PPUSCROLL = 0x2005
        const val PPUADDR = 0x2006
        const val PPUDATA = 0x2007
    }

    private object PPUCTRLFlags {
        const val NAMETABLE_LOW = 0b00000001
        const val NAMETABLE_HIGH = 0b00000010
        const val VRAM_ADDR_INCREMENT = 0b00000100
        const val SPRITE_TABLE_ADDRESS = 0b00001000
        const val BACKGROUND_TABLE_ADDRESS = 0b00010000
        const val SPRITE_SIZE = 0b00100000
        const val PPU_MASTER_SLAVE = 0b01000000
        const val GENERATE_VBLANK_NMI = 0b10000000
    }

    // Registers
    private var ppuctrl: Int = 0
    private var ppumask: Int = 0
    private var ppustatus: Int = 0
    private var oamaddr: Int = 0
    private var oamdata: Int = 0
    private var ppuscroll: Int = 0
    private var ppuaddr: Int = 0    // = v internal register
    private var ppudata: Int = 0    // = buffered VRAM data from previous read

    private val palette: IntArray = IntArray(32)
    private var oamData: IntArray = IntArray(256)

    // private var v: Int = 0  // 15 bits, holds the VRAM address the PPU is about to access
    private var t: Int = 0  // 15 bits, holds a "temporary" VRAM address shared by PPUSCROLL and
                            // PPUADDR
    private var x: Int = 0  // 3 bits, holds the 3 bit X scroll position within a 8x8-pixel tile
    private var w: Boolean = false  // 1 bit flag, first or second write toggle for PPUSCROLL and
                                    // PPUADDR

    private var cycle: Int = 0
    private var scanline: Int = 0

    // https://www.nesdev.org/wiki/PPU_scrolling#$2000_(PPUCTRL)_write
    fun cpuReadRegister(address: Int): Int {
        val register = address - 0x2000
        return when (register) {
            Registers.PPUSTATUS -> {
                w = false
                ppustatus
            }
            Registers.OAMDATA -> oamData[oamaddr]
            Registers.PPUDATA -> {
                val oldValue = ppudata
                ppudata = readMemory(ppuaddr)
                oldValue
            }
            else -> 0
        }
    }

    fun cpuWriteRegister(address: Int, value: Int) {
        val valueByte =  value and 0xFF
        when (address) {
            Registers.PPUCTRL -> {
                ppuctrl = value
                t = (t and 0x73FF) or ((value and 0x03) shl 10)
            }
            Registers.PPUMASK -> ppumask = valueByte
            Registers.OAMADDR -> oamaddr = valueByte
            Registers.OAMDATA -> {
                oamData[oamaddr] = valueByte
                oamaddr.plus8(1)
            }
            Registers.PPUSCROLL -> {
                if (!w) {
                    t = (t and 0x7FE0) or ((valueByte and 0xF8) shr 3)
                    x = valueByte and 0x07
                    w = true
                } else {
                    t = (t and 0x73E0) or ((valueByte and 0x07) shl 12) or
                            ((valueByte and 0xF8) shl 2)
                    w = false
                }
            }
            Registers.PPUADDR -> {
                if (!w) {
                    t = (t and 0xFF) or ((valueByte and 0x3F) shl 8)
                } else {
                    ppuaddr = t or (valueByte and 0xFF)
                }
                w = !w
            }
            Registers.PPUDATA -> {
                writeMemory(ppuaddr, valueByte)
                ppuaddr += if (ppuctrl and PPUCTRLFlags.VRAM_ADDR_INCREMENT > 0) 32 else 1
            }
            else -> throw InvalidOperationException(TAG, "Invalid PPU register write at $address")
        }
    }

    fun loadOamData(data: IntArray) {
        oamData = data.copyOf()
    }
}