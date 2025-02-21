package com.onandor.nesemu.emulation.nes

import java.nio.IntBuffer

/*
 - Pattern table: CHR ROM on the cartridge, defines the shapes (and colors) of the tiles that make
   up the backgrounds and the sprites.
 - Nametable: 1024 byte area in the vram, used to lay out backgrounds. Each byte controls one 8x8
   pixel tile, and each nametable 30 rows of 32 tiles each. There are 4 logical nametables.
 - Attribute table: 64 bytes at the end of each nametable that controls which color palette is
   assigned to each part of the background.
 */

class Ppu(
    private val readExternalMemory: (address: Int) -> Int,
    private val writeExternalMemory: (address: Int, value: Int) -> Unit,
    private val generateNmi: () -> Unit,
    private val frameReady: () -> Unit
) {

    companion object {
        private const val TAG = "Ppu"

        private const val SCREEN_WIDTH = 256
        private const val SCREEN_HEIGHT = 240

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

        // Pattern table constants
        const val TILE_SIZE = 8
        const val GRID_SIZE = 16
        const val TILE_BYTES = 16
    }

    // Registers
    // https://www.nesdev.org/wiki/PPU_registers

    private object Control {
        const val ADDRESS = 0x2000
        var register: Int = 0
        var nametableSelect: Int
            get() { return register and 0x03 }
            set(value) { register = (register and 0xFC) or (value and 0x03) }
        var vramAddrIncrement: Int
            get() { return (register and 0x04) shr 2 }
            set(value) { register = if (value > 0) register or 0x04 else register and 0x04.inv() }
        var spritePatternTableSelect: Int
            get() { return (register and 0x08) shr 3 }
            set(value) { register = if (value > 0) register or 0x08 else register and 0x08.inv() }
        var bgPatternTableSelect: Int
            get() { return (register and 0x10) shr 4 }
            set(value) { register = if (value > 0) register or 0x10 else register and 0x10.inv() }
        var tallSprites: Int
            get() { return (register and 0x20) shr 5 }
            set(value) { register = if (value > 0) register or 0x20 else register and 0x20.inv() }
        var masterSlaveSelect: Int
            get() { return (register and 0x40) shr 6 }
            set(value) { register = if (value > 0) register or 0x40 else register and 0x40.inv() }
        var enableVBlankNmi: Int
            get() { return (register and 0x80) shr 7 }
            set(value) { register = if (value > 0) register or 0x80 else register and 0x80.inv() }
    }

    private object Mask {
        const val ADDRESS = 0x2001
        var register: Int = 0
        var grayscale: Int
            get() { return register and 0x01 }
            set(value) { register = if (value > 0) register or 0x01 else register and 0x01.inv() }
        var showBgInLeft: Int
            get() { return (register and 0x02) shr 1 }
            set(value) { register = if (value > 0) register or 0x02 else register and 0x02.inv() }
        var showSpritesInLeft: Int
            get() { return (register and 0x04) shr 2 }
            set(value) { register = if (value > 0) register or 0x04 else register and 0x04.inv() }
        var backgroundRenderingOn: Int
            get() { return (register and 0x08) shr 3 }
            set(value) { register = if (value > 0) register or 0x08 else register and 0x08.inv() }
        var spriteRenderingOn: Int
            get() { return (register and 0x10) shr 4 }
            set(value) { register = if (value > 0) register or 0x10 else register and 0x10.inv() }
        var emphasizeRed: Int
            get() { return (register and 0x20) shr 5 }
            set(value) { register = if (value > 0) register or 0x20 else register and 0x20.inv() }
        var emphasizeGreen: Int
            get() { return (register and 0x40) shr 6 }
            set(value) { register = if (value > 0) register or 0x40 else register and 0x40.inv() }
        var emphasizeBlue: Int
            get() { return (register and 0x80) shr 7 }
            set(value) { register = if (value > 0) register or 0x80 else register and 0x80.inv() }
    }

    private object Status {
        const val ADDRESS = 0x2002
        var register: Int = 0
        var spriteOverflow: Int
            get() { return (register and 0x20) shr 5 }
            set(value) { register = if (value > 0) register or 0x20 else register and 0x20.inv() }
        var spriteZeroHit: Int
            get() { return (Mask.register and 0x40) shr 6 }
            set(value) { register = if (value > 0) register or 0x40 else register and 0x40.inv() }
        var vblank: Int
            get() { return (Mask.register and 0x80) shr 7 }
            set(value) { register = if (value > 0) register or 0x80 else register and 0x80.inv() }
    }

    private object OAMAddress {
        const val ADDRESS = 0x2003
        var register: Int = 0
    }

    // Information about sprites (64 * 4 bytes)
    // - Byte 1: Sprite Y coordinate
    // - Byte 2: Sprite tile number
    // - Byte 3: Sprite attribute
    // - Byte 4: Sprite X coordinate
    private object OAMData {
        const val ADDRESS = 0x2004
        var data = IntArray(256)
    }

    private object Scroll {
        const val ADDRESS = 0x2005
        var register: Int = 0
    }

    private object Address {
        const val ADDRESS = 0x2006
    }

    private object Data {
        const val ADDRESS = 0x2007
        var register: Int = 0
    }

    // Internal data bus for communicating with the CPU, holds values of previous reads and writes
    // https://www.nesdev.org/wiki/PPU_registers#MMIO_registers
    private var busLatch: Int = 0

    // Color palette that the currently rendered frame uses
    private var palette: IntArray = IntArray(32)

    /*
     Structure of v and t during rendering:
     yyy NN YYYYY XXXXX
     ||| || ||||| +++++-- coarse X
     ||| || +++++-------- coarse Y
     ||| ++-------------- nametable select
     +++----------------- fine Y scroll
    */
    private var v: Int = 0  // 15 bits, holds the VRAM address the PPU is about to access
    private var t: Int = 0  // 15 bits, holds a temporary VRAM address shared by PPUSCROLL and
                            // PPUADDR
    private var fineX: Int = 0  // 3 bits, holds the 3 bit X scroll position within a 8x8-pixel tile
    private var w: Boolean = false  // 1 bit flag, first or second write toggle for PPUSCROLL and
                                    // PPUADDR

    private var cycle: Int = 0
    private var scanline: Int = 261 // Scanline 261 is the pre-render scanline
    private var numFrames: Int = -1 // Pre-render frame

    var mirroring: Mirroring = Mirroring.HORIZONTAL

    // Variables related to tile fetching and rendering
    // Background

    // https://www.nesdev.org/wiki/PPU_scrolling#Tile_and_attribute_fetching
    private var nametableId: Int = 0
    private var attributeId: Int = 0

    // https://www.nesdev.org/wiki/PPU_pattern_tables
    // Tiles are 8x8 pixels
    // These two combined store the color indices of a row of pixels of a tile being rendered
    private var bgTilePatternLow: Int = 0    // First bit plane
    private var bgTilePatternHigh: Int = 0   // Second bit plane

    /*
    16 bit shifters that store the pattern and attribute select bits during rendering for the
    current and next tile.
    They are shifted left by one in each cycle during the data fetching phase.
    Every 8 cycles the data for the next tile on the scanline (8 pixels) are loaded into the lower
    8 bits.

    AAAA AAAA BBBB BBBB
    |||| |||| |||| ||||
    |||| |||| ++++-++++-- Next tile
    ++++-++++------------ Currently rendered tile
     */
    private var bgPatternDataLow: Int = 0
    private var bgPatternDataHigh: Int = 0
    private var bgAttributeDataLow: Int = 0
    private var bgAttributeDataHigh: Int = 0

    // Sprites - a maximum of 8 sprites can be on the same scanline

    // 8 sprites, 4 bytes each
    private var oamBuffer: IntArray = IntArray(32) { 0xFF }
    private var numSpritesOnScanline: Int = 0

    // Between cycles 1-64 it indicates that the secondary OAM is being cleared, so reading 0x2004
    // (OAMDATA) should return 0xFF
    private var oamClear: Boolean = false

    // Maximum of 8 sprites; 8 bits of pattern data for the low and high bitplanes per sprite
    private var sprPatternDataLow: IntArray = IntArray(8)
    private var sprPatternDataHigh: IntArray = IntArray(8)

    lateinit var frame: IntArray
        private set
    private var frameBuffer: IntBuffer = IntBuffer.allocate(SCREEN_WIDTH * SCREEN_HEIGHT)

    // Debug variables
    var dbgDrawPatternTable: Boolean = false
    var dbgPatternTableFrame: IntArray = IntArray(128 * 256)
        private set

    var dbgDrawNametable: Boolean = false
    var dbgNametableFrame: IntArray = IntArray(512 * 480)
        private set

    var dbgColorPaletteId: Int = 0
    var dbgDrawColorPalettes: Boolean = false
    var dbgColorPalettes = Array(8) { IntArray(4 * 225) }
        private set

    fun reset() {
        cycle = 0
        scanline = 261
        numFrames = -1
        v = 0
        t = 0
        fineX = 0
        w = false
        busLatch = 0
        Control.register = 0
        Mask.register = 0
        Status.register = 0
        Scroll.register = 0
        Data.register = 0
        OAMData.data = IntArray(256)
        frameBuffer.clear()
        palette = IntArray(32)
        dbgPatternTableFrame = IntArray(128 * 256)
        dbgNametableFrame = IntArray(512 * 480)
        dbgColorPalettes = Array(8) { IntArray(4 * 225) }
        oamClear = false
        oamBuffer = IntArray(32) { 0xFF }
        sprPatternDataLow = IntArray(8)
        sprPatternDataHigh= IntArray(8)
    }

    fun tick() {
        if (scanline == 261 && cycle == 339 && numFrames % 2 == 1) {
            // Skipping the last cycle of odd frames
            cycle = 0
            scanline = 0
            numFrames++
            return
        }
        if (cycle == 0) {
            // Idle cycle
            cycle = 1
            return
        }

        if (scanline != 261 && scanline >= 240) {
            if (scanline == 241 && cycle == 1) {
                // Start of vertical blank
                Status.vblank = 1
                renderFrame()
                if (Control.enableVBlankNmi > 0) {
                    generateNmi()
                }
            }

            cycle++
            if (cycle == 341) {
                cycle = 0
                scanline++
                if (scanline == 262) {
                    scanline = 0
                    numFrames++
                }
            }
            return
        }

        if (scanline == 261 && cycle == 1) {
            // End of vertical blank, start of pre-render scanline
            Status.vblank = 0
            Status.spriteZeroHit = 0
            Status.spriteOverflow = 0
            sprPatternDataLow = IntArray(8)
            sprPatternDataHigh = IntArray(8)
        }

        fetchTileData()
        if (Mask.spriteRenderingOn + Mask.backgroundRenderingOn > 0) {
            scroll()
        }

        if (cycle == 1) {
            oamClear = true
        } else if (cycle == 64) {
            oamClear = false
        }
        if (cycle == 257 && scanline != 261 && Mask.spriteRenderingOn + Mask.backgroundRenderingOn > 0) {
            evaluateSprites()
        }

        if (scanline != 261 && cycle in 1 .. 256) {
            renderPixel()
        }

        cycle++
        if (cycle == 341) {
            cycle = 0
            scanline++
            if (scanline == 262) {
                scanline = 0
                numFrames++
            }
        }
    }

    private fun renderPixel() {
        val bitSelect = 0x8000 shr fineX
        val offset = 15 - fineX

        // Background pixel

        val bgPixelIdLow = (bgPatternDataLow and bitSelect) ushr offset
        val bgPixelIdHigh = (bgPatternDataHigh and bitSelect) ushr offset
        val bgPixelId = (bgPixelIdHigh shl 1) or bgPixelIdLow

        val bgPaletteIdLow = (bgAttributeDataLow and bitSelect) ushr offset
        val bgPaletteIdHigh = (bgAttributeDataHigh and bitSelect) ushr offset
        val bgPaletteId = (bgPaletteIdHigh shl 1) or bgPaletteIdLow

        // SAAPP
        // |||||
        // |||++- Pixel value from tile pattern data
        // |++--- Palette number from attributes
        // +----- Background/Sprite select
        val bgColor = if (bgPixelId != 0 && Mask.backgroundRenderingOn > 0) {
            COLOR_PALETTE[readMemory(0x3F00 + (bgPaletteId shl 2) or bgPixelId)]
        } else {
            COLOR_PALETTE[readMemory(0x3F00)]
        }

        // Sprite pixel

        // If the color is left on -1 after the priority evaluation, the background is drawn
        var spriteColor: Int = -1

        if (Mask.spriteRenderingOn > 0) {
            for (i in 0 ..< numSpritesOnScanline) {
                if (cycle <= oamBuffer[i * 4 + 3]) {
                    // We have not yet reached the sprite
                    continue
                }

                val sprPixelIdLow = (sprPatternDataLow[i] and 0x80) ushr 7
                val sprPixelIdHigh = (sprPatternDataHigh[i] and 0x80) ushr 7
                val sprPixelId = (sprPixelIdHigh shl 1) or sprPixelIdLow
                if (sprPixelId == 0) {
                    // This pixel of the sprite is transparent
                    continue
                }

                // At this point we have reached an opaque sprite pixel
                // If the priority of the first opaque sprite is set to 1, and the background is
                // NOT transparent, then the background is drawn
                val priority = (oamBuffer[i * 4 + 2] and 0x20) ushr 5
                if (bgPixelId != 0) {
                    // Check for sprite 0 hit
                    if (i == 0 && oamBuffer[0] == OAMData.data[0] && oamBuffer[3]== OAMData.data[3]
                        && Mask.backgroundRenderingOn > 0 && Status.spriteZeroHit == 0) {
                        val lowerCycleLimit =
                            if (Mask.showBgInLeft == 0 || Mask.showSpritesInLeft == 0) 8 else 1
                        if (cycle - 1 in lowerCycleLimit .. 254) {
                            Status.spriteZeroHit = 1
                        }
                    }
                    if (priority == 1) {
                        // The background pixel has priority
                        break;
                    }
                }

                val sprPaletteId = oamBuffer[i * 4 + 2] and 0x03
                // The sprite is opaque and its priority is 0 -> draw the sprite pixel
                spriteColor = COLOR_PALETTE[readMemory(0x3F10 + (sprPaletteId shl 2) + sprPixelId)]
                break
            }
        }

        frameBuffer.put(if (spriteColor != -1) spriteColor else bgColor)
    }

    private fun renderFrame() {
        if (dbgDrawPatternTable) {
            dbgPatternTableFrame = dbgRenderPatternTable()
        }
        if (dbgDrawNametable) {
            dbgNametableFrame = dbgRenderNametables()
        }
        if (dbgDrawColorPalettes) {
            for (i in 0 ..< 8) {
                dbgColorPalettes[i] = dbgRenderColorPalette(i)
            }
        }
        frame = frameBuffer.array().copyOf()
        frameBuffer.clear()
        frameReady()
    }

    private fun fetchTileData() {
        if (cycle in 1 .. 256 || cycle in 321 .. 336) {
            // Fetching background tile data in cycles 1 .. 256 and prefetching data for 2 the
            // first 2 tiles on the next scanline in 321 .. 336
            shiftPatternData()
            when (cycle % 8) {
                1 -> {
                    bgPatternDataLow = (bgPatternDataLow and 0xFF00) or bgTilePatternLow
                    bgPatternDataHigh = (bgPatternDataHigh and 0xFF00) or bgTilePatternHigh

                    // Extrapolate the attribute bits to cover the whole tile
                    val attributeDataLow = if (attributeId and 0b01 > 0) 0xFF else 0x00
                    bgAttributeDataLow = (bgAttributeDataLow and 0xFF00) or attributeDataLow
                    val attributeDataHigh = if (attributeId and 0b10 > 0) 0xFF else 0x00
                    bgAttributeDataHigh = (bgAttributeDataHigh and 0xFF00) or attributeDataHigh

                    nametableId = readMemory(0x2000 or (v and 0x0FFF))
                }
                3 -> {
                    val address = 0x23C0 or (v and 0x0C00) or ((v ushr 4) and 0x38) or
                            ((v ushr 2) and 0x07)

                    attributeId = readMemory(address)

                    val coarseX = (v and 0x001F)
                    val coarseY = (v and 0x03E0) ushr 5
                    if (coarseX and 0x02 > 0) {
                        attributeId = attributeId ushr 2
                    }
                    if (coarseY and 0x02 > 0) {
                        attributeId = attributeId ushr 4
                    }

                    attributeId = attributeId and 0b111
                }
                5 -> {
                    val basePatternTable = 0x1000 * Control.bgPatternTableSelect
                    val fineY = (v ushr 12) and 0x07
                    val address = basePatternTable or (nametableId shl 4) or fineY
                    bgTilePatternLow = readMemory(address)
                }
                7 -> {
                    val basePatternTable = 0x1000 * Control.bgPatternTableSelect
                    val fineY = (v ushr 12) and 0x07
                    val address = (basePatternTable or (nametableId shl 4) or fineY) + 8
                    bgTilePatternHigh = readMemory(address)
                }
            }
        } else if (cycle in 257 .. 320) {
            // Prefetching tile data for the sprites on the next scanline
            when (cycle % 8) {
                5, 7 -> {
                    fetchSpriteTileData()
                }
            }
        }
    }

    private fun fetchSpriteTileData() {
        // 0 .. 8 index of the sprite in the secondary OAM
        val spriteIndex = (cycle - 257) / 8

        val tileY = oamBuffer[spriteIndex * 4]
        var tileIndex = oamBuffer[spriteIndex * 4 + 1]
        val tileAttributes = oamBuffer[spriteIndex * 4 + 2]

        var basePatternTable: Int
        var rowShift = 0
        if (Control.tallSprites > 0) {
            // 8x16 sprites: the base attribute table is calculated from the LSB of
            // the tile index, which is then not used when indexing into said
            // attribute table
            basePatternTable = 0x1000 * (tileIndex and 0x01)
            tileIndex = tileIndex and 0xFE
            // Bottom 8x8 tile of the sprite -> shift down by one row
            rowShift = (scanline - tileY >= 8).toInt()
        } else {
            basePatternTable = 0x1000 * Control.spritePatternTableSelect
        }

        var address = if (tileAttributes and 0x80 > 0) {
            // Tile is flipped vertically
            basePatternTable or ((tileIndex + rowShift) * 16) or (7 - (scanline - tileY))
        } else {
            basePatternTable or ((tileIndex + rowShift) * 16) or (scanline - tileY)
        }

        // This function gets called on cycle mod 8 = 5 and cycle mod 8 = 7
        // The low byte of the tile is fetched on 5 and the high byte on 7
        if (cycle % 8 == 7) {
            address += 8
        }

        var tilePattern = readMemory(address)
        if ((tileAttributes and 0x40) > 0) {
            // Tile is flipped horizontally -> reverse the order of bits
            var reversed = 0
            for (i in 0 ..< 8) {
                reversed = (reversed shl 1) or (tilePattern and 0x01)
                tilePattern = tilePattern ushr 1
            }
            tilePattern = reversed and 0xFF
        }

        if (cycle % 8 == 5) {
            sprPatternDataLow[spriteIndex] = tilePattern
        } else {
            sprPatternDataHigh[spriteIndex] = tilePattern
        }
    }

    private fun shiftPatternData() {
        bgPatternDataLow = bgPatternDataLow shl 1
        bgPatternDataHigh = bgPatternDataHigh shl 1
        bgAttributeDataLow = bgAttributeDataLow shl 1
        bgAttributeDataHigh = bgAttributeDataHigh shl 1

        if (cycle in 1 .. 256) {
            // Check all 8 (or less, because dummy 0xFF bits) sprites in secondary OAM
            for (i in 0 ..< 8) {
                if (cycle - 1 > oamBuffer[i * 4 + 3]) {
                    // Sprite X coordinate reached, shift its pattern data
                    sprPatternDataLow[i] = sprPatternDataLow[i] shl 1
                    sprPatternDataHigh[i] = sprPatternDataHigh[i] shl 1
                }
            }
        }
    }

    // Increment the fine and coarse X and Y registers inside v while rendering
    private fun scroll() {
        if (cycle == 256) {
            // Increment the vertical position (fine Y) in v, overflowing to coarse Y if necessary
            // https://www.nesdev.org/wiki/PPU_scrolling#Y_increment
            if ((v and 0x7000) != 0x7000) {                 // if fine Y < 7
                v += 0x1000                                 // increment fine Y
            } else {
                v = v and 0x7000.inv()                      // fine Y = 0
                var coarseY = (v and 0x03E0) ushr 5          // extract coarse Y position
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
        } else if (cycle == 257) {
            // Copy coarse X and lower nametable byte from t into v
            v = (v and 0xFBE0) or (t and 0x041F)
        }

        // Copy coarse Y, fine Y and upper nametable byte from t into v
        if (scanline == 261 && cycle in 280 .. 304) {
            v = (v and 0x841F) or (t and 0x7BE0)
        }

        // Cycle 328 of current scanline - cycle 256 of next scanline
        if (cycle !in 257 .. 327) {
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

    // Cheap out on the implementation and evaluate all sprites all at once
    // https://www.nesdev.org/wiki/PPU_sprite_evaluation#Details
    private fun evaluateSprites() {
        oamBuffer = IntArray(32) { 0xFF }
        numSpritesOnScanline = 0
        for (n in 0 ..< 64) {
            val y = OAMData.data[n * 4]
            val height = 8 + Control.tallSprites * 8
            if (scanline - y !in 0 ..< height) {
                // The sprite doesn't have a row of pixels on the next scanline, skipping to the next
                continue
            }
            if (numSpritesOnScanline < 8) {
                for (m in 0 ..< 4) {
                    oamBuffer[numSpritesOnScanline * 4 + m] = OAMData.data[n * 4 + m]
                }
                numSpritesOnScanline += 1
            } else {
                Status.spriteOverflow = 1
            }
        }
        if (numSpritesOnScanline < 8) {
            // Since on real hardware the evaluation works by copying the y position of the sprite
            // into the secondary OAM and then evaluating, if the secondary OAM is not full,
            // the y position of the last sprite in the OAM should be the last non 0xFF value
            oamBuffer[numSpritesOnScanline * 4] = OAMData.data[63 * 4]
        }
    }

    private fun readMemory(address: Int): Int {
        return if (address >= 0x3F00) {
            var paletteAddress = address and 0x1F
            // Mirror 0x3F10 -> 0x3F00, 0x3F14 -> 0x3F04, 0x3F18 -> 0x3F08, 0x3F1C -> 0x3F0C
            // https://forums.nesdev.org/viewtopic.php?t=892
            if (paletteAddress >= 0x10 && paletteAddress % 4 == 0) {
                paletteAddress -= 0x10
            }
            palette[paletteAddress]
        } else {
            readExternalMemory(address)
        }
    }

    private fun writeMemory(address: Int, value: Int) {
        if (address >= 0x3F00) {
            var paletteAddress = address and 0x1F
            if (paletteAddress >= 16 && paletteAddress % 4 == 0) {
                paletteAddress -= 16
            }
            palette[paletteAddress] = value
        } else {
            writeExternalMemory(address, value)
        }
    }

    // https://www.nesdev.org/wiki/PPU_registers
    // https://www.nesdev.org/wiki/PPU_scrolling#Register_controls
    fun cpuReadRegister(address: Int): Int {
        return when (address) {
            Status.ADDRESS -> {
                w = false
                val status = Status.register
                Status.vblank = 0
                busLatch = status
                status
            }
            OAMData.ADDRESS -> {
                busLatch = if (oamClear) 0xFF else OAMData.data[OAMAddress.register]
                busLatch
            }
            Data.ADDRESS -> {
                var data = Data.register
                Data.register = readMemory(v)
                // Palette reads return values in the same cycle
                if (v >= 0x3F00) {
                    data = Data.register
                }
                v += if (Control.vramAddrIncrement > 0) 32 else 1
                busLatch = data
                data
            }
            else -> busLatch
        }
    }

    // TODO (?): ignore writes to specific registers until X CPU cycles
    // https://www.nesdev.org/wiki/PPU_registers - second paragraph
    fun cpuWriteRegister(address: Int, value: Int) {
        val valueByte = value and 0xFF
        busLatch = valueByte
        when (address) {
            Control.ADDRESS -> {
                Control.register = value
                // Transfer the nametable select bytes from Control into the temporary address
                t = (t and 0x73FF) or (Control.nametableSelect shl 10)
                if (Control.enableVBlankNmi > 0 && Status.vblank > 0) {
                    generateNmi()
                }
            }
            Mask.ADDRESS -> Mask.register = valueByte
            OAMAddress.ADDRESS -> OAMAddress.register = valueByte
            OAMData.ADDRESS -> {
                OAMData.data[OAMAddress.register] = valueByte
                OAMAddress.register.plus8(1)
            }
            Scroll.ADDRESS -> {
                if (!w) {
                    t = (t and 0x7FE0) or ((valueByte and 0xF8) ushr 3)
                    fineX = valueByte and 0x07
                } else {
                    t = (t and 0x8C1F) or ((valueByte and 0x07) shl 12) or
                            ((valueByte and 0xF8) shl 2)
                }
                w = !w
            }
            Address.ADDRESS -> {
                if (!w) {
                    // The first write sets the high byte of the temporary address register
                    t = (t and 0xFF) or ((valueByte and 0x3F) shl 8)
                } else {
                    // The second write sets the low byte and transfers the value into the address
                    // register
                    t = (t and 0xFF00) or (valueByte and 0xFF)
                    v = t
                }
                w = !w
            }
            Data.ADDRESS -> {
                writeMemory(v, valueByte)
                v += if (Control.vramAddrIncrement > 0) 32 else 1
            }
        }
    }

    fun loadOamData(data: IntArray) {
        OAMData.data = data.copyOf()
    }

    // Functions used for debugging

    fun dbgCpuReadRegister(address: Int): Int {
        return when (address) {
            Status.ADDRESS -> Status.register
            OAMData.ADDRESS -> OAMData.data[OAMAddress.register]
            Data.ADDRESS -> Data.register
            else -> busLatch
        }
    }

    private fun dbgRenderPatternTable(): IntArray {
        val patternTable = IntArray(256 * 128)
        // Left grid (0x0000 - 0x0FFF)
        for (tileRow in 0 until GRID_SIZE) {
            for (tileCol in 0 until GRID_SIZE) {
                dbgRenderPatternTableTile(patternTable, tileRow, tileCol, 0x0000)
            }
        }
        // Right grid (0x1000 - 0x1FFF)
        for (tileRow in 0 until GRID_SIZE) {
            for (tileCol in 0 until GRID_SIZE) {
                dbgRenderPatternTableTile(patternTable, tileRow, tileCol + GRID_SIZE, 0x1000)
            }
        }
        return patternTable
    }

    private fun dbgRenderPatternTableTile(
        grid: IntArray,
        tileRow: Int,
        tileCol: Int,
        gridBaseAddress: Int
    ) {
        val tileIndex = tileRow * GRID_SIZE + tileCol % GRID_SIZE
        val tileAddress = gridBaseAddress + tileIndex * TILE_BYTES

        for (y in 0 until TILE_SIZE) {
            val lowByte = readMemory(tileAddress + y)
            val highByte = readMemory(tileAddress + y + 8)

            for (x in 0 until TILE_SIZE) {
                val pixel = (lowByte shr (7 - x) and 1) or
                        ((highByte shr (7 - x) and 1) shl 1)

                val pixelIdx = (tileRow * TILE_SIZE + y) * (GRID_SIZE * TILE_SIZE * 2) +
                        (tileCol * TILE_SIZE + x)
                grid[pixelIdx] =
                    COLOR_PALETTE[readMemory(0x3F00 + ((dbgColorPaletteId shl 2) or pixel))]
            }
        }
    }

    private fun dbgRenderNametables(): IntArray {
        val frame = IntArray(512 * 480)
        dbgRenderNametable(frame, 0, 0, 0)     // Nametable 1 - Top left
        dbgRenderNametable(frame, 1, 0, 256)   // Nametable 2 - Top right
        dbgRenderNametable(frame, 2, 240, 0)   // Nametable 3 - Bottom left
        dbgRenderNametable(frame, 3, 240, 256) // Nametable 4 - Bottom right
        return frame
    }

    private fun dbgRenderNametable(frame: IntArray, nametableIdx: Int, rowOffset: Int, colOffset: Int) {
        val baseAddress = 0x2000 + nametableIdx * 0x400
        for (tileIdx in 0 until 960) {
            val tileIndex = readMemory(baseAddress + tileIdx)
            val tileAddress = tileIndex * TILE_BYTES + 0x1000 * Control.bgPatternTableSelect

            val tileRow = tileIdx / 32
            val tileCol = tileIdx % 32
            dbgRenderNametableTile(frame, tileAddress, tileRow, tileCol, rowOffset, colOffset)
        }
    }

    private fun dbgRenderNametableTile(
        frame: IntArray,
        tileAddress: Int,
        tileRow: Int,
        tileCol: Int,
        rowOffset: Int,
        colOffset: Int
    ) {
        for (y in 0 until TILE_SIZE) {
            val lowByte = readMemory(tileAddress + y)
            val highByte = readMemory(tileAddress + y + 8)
            for (x in 0 until TILE_SIZE) {
                val pixel = (lowByte shr (7 - x) and 1) or
                        ((highByte shr (7 - x) and 1) shl 1)

                val pixelIdx = (rowOffset + tileRow * TILE_SIZE + y) * 512 +
                        (colOffset + tileCol * TILE_SIZE + x)
                frame[pixelIdx] =
                    COLOR_PALETTE[readMemory(0x3F00 + ((dbgColorPaletteId shl 2) or pixel))]
            }
        }
    }

    private fun dbgRenderColorPalette(paletteId: Int): IntArray {
        // 4 colors next to each other, each 15 x 15 pixels
        val palette = IntArray(4 * 225)
        for (colorIdx in 0 ..< 4) {
            for (y in 0 ..< 15) {
                for (x in 0 ..< 15) {
                    palette[(colorIdx * 15) + y * 60 + x] =
                        COLOR_PALETTE[readMemory(0x3F00 + paletteId * 4 + colorIdx)]
                }
            }
        }
        return palette
    }
}