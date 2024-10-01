package com.onandor.nesemu.nes

class Ppu(
    private val readMemory: (address: Int) -> Int,
    private val writeMemory: (address: Int, value: Int) -> Unit,
    private val generateNmi: () -> Unit
) {

    companion object {
        private const val TAG = "Ppu"

        private const val SCREEN_HEIGHT = 256
        private const val SCREEN_WIDTH = 240
        private const val LAST_CYCLE = 340
        private const val POST_RENDER_SCANLINE = 240
        private const val VBLANK_START_SCANLINE = 241
        private const val PRE_RENDER_SCANLINE = 261

        private val COLOR_PALETTE = arrayOf(
            0x59595F, 0x03008A, 0x17008A, 0x3A0673, 0x4E0B52, 0x4E0C12, 0x4E0C03, 0x402405,
            0x333308, 0x193207, 0x103215, 0x184344, 0x194364, 0x000000, 0x080808, 0x080808,
            0xAAAAAA, 0x1C42D6, 0x5016E6, 0x6F12E6, 0x8E1CB5, 0x9D2154, 0x8E3B10, 0x804812,
            0x666619, 0x3E6518, 0x286417, 0x286456, 0x215385, 0x080808, 0x080808, 0x080808,
            0xEEEEEE, 0x5586F8, 0x7877F8, 0x9049F7, 0xAF4DE7, 0xBF5D96, 0xD06E4A, 0xC38C29,
            0xB8AB31, 0x85B934, 0x55B83D, 0x56B87C, 0x56B8CA, 0x444444, 0x080808, 0x080808,
            0xEEEEEE, 0x5586F8, 0xAAAAFA, 0xB69AF9, 0xD49CFA, 0xE39DDA, 0xE5ADAB, 0xE7BD9D,
            0xEBDE91, 0xC1DC90, 0xA6DB9F, 0xA6DBBD, 0xA7DBEC, 0xAAAAAA, 0x080808, 0x080808
        )
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
        const val BASE_NAMETABLE = 0b00000011
        const val VRAM_ADDR_INCREMENT = 0b00000100
        const val SPRITE_TABLE_ADDRESS = 0b00001000
        const val BG_TABLE_ADDRESS = 0b00010000
        const val SPRITE_SIZE = 0b00100000
        const val PPU_MASTER_SLAVE = 0b01000000
        const val GENERATE_VBLANK_NMI = 0b10000000
    }

    private object PPUMASKFlags {
        const val GRAYSCALE = 0b00000001
        const val SHOW_BG_IN_LEFT = 0b00000010
        const val SHOW_SPRITES_IN_LEFT = 0b00000100
        const val SHOW_BACKGROUND = 0b00001000
        const val SHOW_SPRITES = 0b00010000
        const val EMPHASIZE_RED = 0b00100000
        const val EMPHASIZE_GREEN = 0b01000000
        const val EMPHASIZE_BLUE = 0b10000000
    }

    private object PPUSTATUSFlags {
        const val SPRITE_OVERFLOW = 0b00100000
        const val SPRITE_0_HIT = 0b01000000
        const val IN_VBLANK = 0b10000000
    }

    // Registers
    private var ppuctrl: Int = 0
    private var ppumask: Int = 0
    private var ppustatus: Int = 0b1010000
    private var oamaddr: Int = 0
    private var oamdata: Int = 0
    private var ppuscroll: Int = 0
    private var ppuaddr: Int = 0
    private var ppudata: Int = 0    // = buffered VRAM data from previous read

    // Color palette that the currently rendered frame uses
    private var framePalette: IntArray = IntArray(32)

    // Information about sprites (64 * 4 bytes)
    // - Byte 1: Sprite Y coordinate
    // - Byte 2: Sprite tile number
    // - Byte 3: Sprite attribute
    // - Byte 4: Sprite X coordinate
    private var oamData: IntArray = IntArray(256)


    /*
     Structure of v and t during rendering:
     yyy NN YYYYY XXXXX
     ||| || ||||| +++++-- high 3 bits of coarse X (x/4)
     ||| || +++++-------- high 3 bits of coarse Y (y/4)
     ||| ++-------------- nametable select
     +++----------------- fine Y scroll
    */
    private var v: Int = 0  // 15 bits, holds the VRAM address the PPU is about to access
    private var t: Int = 0  // 15 bits, holds a "temporary" VRAM address shared by PPUSCROLL and
                            // PPUADDR
    private var x: Int = 0  // 3 bits, holds the 3 bit X scroll position within a 8x8-pixel tile
    private var w: Boolean = false  // 1 bit flag, first or second write toggle for PPUSCROLL and
                                    // PPUADDR

    private var cycle: Int = 0
    private var scanline: Int = PRE_RENDER_SCANLINE // Scanline 261 is the pre-render scanline
    private var frame: Int = -1 // Pre-render frame

    var mirroring: Mirroring = Mirroring.HORIZONTAL
    private var isRenderingEnabled: Boolean = true

    private var nametableByte: Int = 0
    private var attributeTableByte: Int = 0
    private var patternTableTileLow: Int = 0
    private var patternTableTileHigh: Int = 0

    fun reset() {
        cycle = 0
        scanline = PRE_RENDER_SCANLINE
        frame = -1
        v = 0
        t = 0
        x = 0
        w = false
        ppuctrl = 0
        ppumask = 0
        ppustatus = ppustatus and 0b1000000
        ppuscroll = 0
        ppudata = 0
        oamData = IntArray(256)
        isRenderingEnabled = true
    }

    fun tick() {
        if (!isRenderingEnabled) {
            cycle = (cycle + 1) % LAST_CYCLE
            if (cycle == 0) {
                scanline = (scanline + 1) % PRE_RENDER_SCANLINE
                if (scanline == 0) {
                    frame++
                }
            }
            return
        }
        if (scanline == PRE_RENDER_SCANLINE && cycle == LAST_CYCLE - 1 && frame % 2 == 1) {
            // Skipping the last cycle of odd frames
            cycle = 0
            scanline = 0
            frame++
            return
        }
        if (cycle == 0) {
            // TODO: might not be necessary
            // Idle cycle
            cycle++
            return
        }
        if (scanline >= POST_RENDER_SCANLINE) {
            if (scanline == VBLANK_START_SCANLINE && cycle == 1) {
                // Start of vertical blank
                ppustatus = ppustatus or PPUSTATUSFlags.IN_VBLANK
                if (ppuctrl and PPUCTRLFlags.GENERATE_VBLANK_NMI > 0) {
                    generateNmi()
                }
            } else if (scanline == PRE_RENDER_SCANLINE && cycle == 1) {
                // End of vertical blank
                ppustatus = ppustatus and PPUSTATUSFlags.IN_VBLANK.inv()
            }

            cycle = (cycle + 1) % LAST_CYCLE
            if (cycle == 0) {
                scanline++
            }
            return
        }

        when (cycle) {
            in 1 .. 256 -> {
                // Fetch background tile data
                when (cycle % 8) {
                    1 -> {
                        nametableByte = readMemory(0x2000 or (v and 0x0FFF))
                    }
                    3 -> {
                        val address = 0x23C0 or (v and 0x0C00) or ((v ushr 4) and 0x38) or
                                ((v ushr 2) and 0x07)
                        attributeTableByte = readMemory(address)
                    }
                    5 -> {
                        val basePatternTable = 0x1000 * ((ppuctrl and PPUCTRLFlags.BG_TABLE_ADDRESS) shr 4)
                        val fineY = (v ushr 12) and 0x07
                        val address = basePatternTable or (nametableByte shl 4) or fineY
                        patternTableTileLow = readMemory(address)
                    }
                    7 -> {
                        val basePatternTable = 0x1000 * ((ppuctrl and PPUCTRLFlags.BG_TABLE_ADDRESS) shr 4)
                        val fineY = (v ushr 12) and 0x07
                        val address = (basePatternTable or (nametableByte shl 4) or fineY) + 8
                        patternTableTileHigh = readMemory(address)
                    }
                }
            }
            in 257 .. 320 -> {
                // Prefetch tile data for the sprites on the next scanline
                when (cycle % 8) {
                    1, 3 -> {
                        // Garbage nametable byte
                        nametableByte = readMemory(0x2000 or (v and 0x0FFF))
                    }
                    5 -> {
                        // Pattern table tile low
                    }
                    7 -> {
                        // Pattern table tile high
                    }
                }
            }
            in 321 .. 336 -> {
                // Prefetch data for the first 2 background tiles of the next scanline
                when (cycle % 8) {
                    1 -> {
                        nametableByte = readMemory(0x2000 or (v and 0x0FFF))
                    }
                    3 -> {
                        // Attribute table byte
                    }
                    5 -> {
                        // Pattern table tile low
                    }
                    7 -> {
                        // Pattern table tile high
                    }
                }
            }
            in 337 ..340 -> {
                if (cycle % 8 == 1 || cycle % 8 == 3) {
                    nametableByte = readMemory(0x2000 or (v and 0x0FFF))
                }
            }
        }

        scroll()

        cycle = (cycle + 1) % LAST_CYCLE
        if (cycle == 0) {
            scanline = (scanline + 1) % PRE_RENDER_SCANLINE
            if (scanline == 0) {
                frame++
            }
        }
    }

    private fun scroll() {
        when (cycle) {
            256 -> {
                // Increment the vertical position (fine Y) in v, overflowing to coarse Y if necessary
                // https://www.nesdev.org/wiki/PPU_scrolling#Y_increment
                if ((v and 0x7000) != 0x7000) {                 // if fine Y < 7
                    v += 0x1000                                 // increment fine Y
                } else {
                    v = v and 0x7000.inv()                      // fine Y = 0
                    var coarseY = (v and 0x03E0) shr 5          // extract coarse Y position
                    if (coarseY == 29) {                        // last row of current nametable
                        coarseY = 0                             // wrap back to 0
                        v = v xor 0x0800                        // switch vertical nametable
                    } else if (coarseY == 31) {
                        coarseY = 0                             // when out of bounds, only wrap around
                    } else {
                        coarseY += 1                            // else increment by one
                    }
                    v = (v and 0x03E0.inv()) or (coarseY shl 5) // put coarseY back into v
                }
            }
            257 -> {
                // Copy all bits related to horizontal position from t into v
                v = (v and 0xFBE0) or (t and 0x041F)
            }
            in 280 .. 304 -> {
                // TODO: might not be optimal
                // Copy remaining bits from t into v
                if (scanline == PRE_RENDER_SCANLINE) {
                    v = (v and 0x841F) or (t and 0x7BE0)
                }
            }
            // Cycle 328 of current scanline - cycle 256 of next scanline
            !in 257 .. 327 -> {
                if (cycle % 8 == 0) {
                    // Increment the horizontal position (coarse X) in v every 8 cycles
                    if ((v and 0x1F) == 31) {   // if coarse X == 31
                        v = v and 0x1F.inv()    // coarse X = 0
                        v = v xor 0x0400        // switch horizontal nametable
                    } else {
                        v += 1                  // increment coarse X
                    }
                }
            }
        }
    }

    // Cycles 321 - 336
    private fun prefetchSprites() {
        when (cycle % 8) {
            1 -> {
                // nametableByte = ?
            }
            3 -> {
                // attributeTableByte = ?
            }
            5 -> {
                // patterTableTileLow = ?
            }
            7 -> {
                // patternTableTileHigh = ?
            }
        }
    }

    // https://www.nesdev.org/wiki/PPU_scrolling#$2000_(PPUCTRL)_write
    fun cpuReadRegister(address: Int): Int {
        val register = address - 0x2000
        return when (register) {
            Registers.PPUSTATUS -> {
                w = false
                val oldStatus = ppustatus
                ppustatus = ppustatus and 0x7F
                oldStatus
            }
            Registers.OAMDATA -> oamData[oamaddr]
            Registers.PPUDATA -> {
                val oldValue = ppudata
                ppudata = readMemory(v)
                oldValue
            }
            else -> throw InvalidOperationException(TAG,
                "Attempting to read write-only PPU register at $address")
        }
    }

    // TODO (?): ignore writes to specific registers until X CPU cycles
    // https://www.nesdev.org/wiki/PPU_registers - second paragraph
    fun cpuWriteRegister(address: Int, value: Int) {
        val valueByte =  value and 0xFF
        when (address) {
            Registers.PPUCTRL -> {
                ppuctrl = value
                t = (t and 0x73FF) or ((value and 0x03) shl 10)
                if (ppuctrl and PPUCTRLFlags.GENERATE_VBLANK_NMI > 0 &&
                    ppustatus and PPUSTATUSFlags.IN_VBLANK > 0) {
                    generateNmi()
                }
            }
            Registers.PPUMASK -> {
                ppumask = valueByte
                isRenderingEnabled = ppumask and
                        (PPUMASKFlags.SHOW_SPRITES or PPUMASKFlags.SHOW_BACKGROUND) > 0
            }
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
                    v = t or (valueByte and 0xFF)
                }
                w = !w
            }
            Registers.PPUDATA -> {
                writeMemory(v, valueByte)
                v += if (ppuctrl and PPUCTRLFlags.VRAM_ADDR_INCREMENT > 0) 32 else 1
            }
            else -> throw InvalidOperationException(TAG,
                "Attempting to write read-only PPU register at $address")
        }
    }

    fun loadOamData(data: IntArray) {
        oamData = data.copyOf()
    }
}