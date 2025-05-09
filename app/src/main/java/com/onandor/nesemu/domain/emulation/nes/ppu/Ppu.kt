package com.onandor.nesemu.domain.emulation.nes.ppu

import com.onandor.nesemu.domain.emulation.nes.plus8
import com.onandor.nesemu.domain.emulation.nes.toInt
import com.onandor.nesemu.domain.emulation.savestate.PpuState
import com.onandor.nesemu.domain.emulation.savestate.Savable
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
    private val onReadExternalMemory: (address: Int) -> Int,
    private val onWriteExternalMemory: (address: Int, value: Int) -> Unit,
    private val onGenerateNMI: () -> Unit,
    private val onFrameReady: (
        frame: IntArray,
        patternTable: IntArray,
        nametable: IntArray,
        colorPalettes: Array<IntArray>) -> Unit
) : Savable<PpuState> {

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
    private val controlReg = Control()
    private val maskReg = Mask()
    private val statusReg = Status()
    private val oamAddressReg = OAMAddress()
    private val oamDataReg = OAMData()
    private val scrollReg = Scroll()
    private val addressReg = Address()
    private val dataReg = Data()

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
    private var scanline: Int = 261  // Scanline 261 is the pre-render scanline
    private var oddFrame: Boolean = false

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

    // Information about sprites (64 * 4 bytes)
    // - Byte 1: Sprite Y coordinate
    // - Byte 2: Sprite tile number
    // - Byte 3: Sprite attribute
    // - Byte 4: Sprite X coordinate
    private var oamData: IntArray = IntArray(256)

    // Between cycles 1-64 it indicates that the secondary OAM is being cleared, so reading 0x2004
    // (OAMDATA) should return 0xFF
    private var oamClear: Boolean = false

    // Maximum of 8 sprites; 8 bits of pattern data for the low and high bitplanes per sprite
    private var sprPatternDataLow: IntArray = IntArray(8)
    private var sprPatternDataHigh: IntArray = IntArray(8)

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
        controlReg.value = 0
        maskReg.value = 0
        statusReg.value = 0
        oamAddressReg.value = 0
        scrollReg.value = 0
        dataReg.value = 0
        busLatch = 0
        palette = IntArray(32)
        v = 0
        t = 0
        fineX = 0
        w = false
        cycle = 0
        scanline = 261
        oddFrame = false
        nametableId = 0
        attributeId = 0
        bgTilePatternLow = 0
        bgTilePatternHigh = 0
        bgPatternDataLow = 0
        bgPatternDataHigh = 0
        bgAttributeDataLow = 0
        bgAttributeDataHigh = 0
        oamBuffer = IntArray(32) { 0xFF }
        numSpritesOnScanline = 0
        oamData = IntArray(256)
        oamClear = false
        sprPatternDataLow = IntArray(8)
        sprPatternDataHigh= IntArray(8)
        frameBuffer.clear()
        dbgPatternTableFrame = IntArray(128 * 256)
        dbgNametableFrame = IntArray(512 * 480)
        dbgColorPalettes = Array(8) { IntArray(4 * 225) }
    }

    fun tick() {
        if (scanline == 261 && cycle == 339 && oddFrame) {
            // Skipping the last cycle of odd frames
            cycle = 0
            scanline = 0
            oddFrame = false
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
                statusReg.vblank = 1
                renderFrame()
                if (controlReg.enableVBlankNmi > 0) {
                    onGenerateNMI()
                }
            }

            cycle++
            if (cycle == 341) {
                cycle = 0
                scanline++
                if (scanline == 262) {
                    scanline = 0
                    oddFrame = !oddFrame
                }
            }
            return
        }

        if (scanline == 261 && cycle == 1) {
            // End of vertical blank, start of pre-render scanline
            statusReg.vblank = 0
            statusReg.spriteZeroHit = 0
            statusReg.spriteOverflow = 0
            sprPatternDataLow = IntArray(8)
            sprPatternDataHigh = IntArray(8)
        }

        fetchTileData()
        if (maskReg.spriteRenderingOn + maskReg.backgroundRenderingOn > 0) {
            scroll()
        }

        if (cycle == 1) {
            oamClear = true
        } else if (cycle == 64) {
            oamClear = false
        }
        if (cycle == 257
            && scanline != 261
            && maskReg.spriteRenderingOn + maskReg.backgroundRenderingOn > 0) {
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
                oddFrame = !oddFrame
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
        val bgColor = if (bgPixelId != 0 && maskReg.backgroundRenderingOn > 0) {
            COLOR_PALETTE[readMemory(0x3F00 + (bgPaletteId shl 2) or bgPixelId)]
        } else {
            COLOR_PALETTE[readMemory(0x3F00)]
        }

        // Sprite pixel

        // If the color is left on -1 after the priority evaluation, the background is drawn
        var spriteColor: Int = -1

        if (maskReg.spriteRenderingOn > 0) {
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
                    if (i == 0 && oamBuffer[0] == oamData[0] && oamBuffer[3] == oamData[3]
                        && maskReg.backgroundRenderingOn > 0 && statusReg.spriteZeroHit == 0) {
                        val lowerCycleLimit =
                            if (maskReg.showBgInLeft == 0 || maskReg.showSpritesInLeft == 0) 8 else 1
                        if (cycle - 1 in lowerCycleLimit .. 254) {
                            statusReg.spriteZeroHit = 1
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

        onFrameReady(frameBuffer.array(), dbgPatternTableFrame, dbgNametableFrame, dbgColorPalettes)
        frameBuffer.clear()
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
                    val basePatternTable = 0x1000 * controlReg.bgPatternTableSelect
                    val fineY = (v ushr 12) and 0x07
                    val address = basePatternTable or (nametableId shl 4) or fineY
                    bgTilePatternLow = readMemory(address)
                }
                7 -> {
                    val basePatternTable = 0x1000 * controlReg.bgPatternTableSelect
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

        val basePatternTable: Int
        var rowShift = 0
        if (controlReg.tallSprites > 0) {
            // 8x16 sprites: the base attribute table is calculated from the LSB of
            // the tile index, which is then not used when indexing into said
            // attribute table
            basePatternTable = 0x1000 * (tileIndex and 0x01)
            tileIndex = tileIndex and 0xFE
            // Bottom 8x8 tile of the sprite -> shift down by one row
            rowShift = (scanline - tileY >= 8).toInt()
        } else {
            basePatternTable = 0x1000 * controlReg.spritePatternTableSelect
        }

        var address = if (tileAttributes and 0x80 > 0) {
            // Tile is flipped vertically
            basePatternTable or ((tileIndex + rowShift) * 16) or (7 - ((scanline - tileY) and 0x07))
        } else {
            basePatternTable or ((tileIndex + rowShift) * 16) or ((scanline - tileY) and 0x07)
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
            // Check all 8 (or fewer) sprites in secondary OAM
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
        sprPatternDataLow = IntArray(8)
        sprPatternDataHigh = IntArray(8)

        for (n in 0 ..< 64) {
            val y = oamData[n * 4]
            val height = 8 + controlReg.tallSprites * 8
            if (scanline - y !in 0 ..< height) {
                // The sprite doesn't have a row of pixels on the next scanline, skipping to the next
                continue
            }
            if (numSpritesOnScanline < 8) {
                for (m in 0 ..< 4) {
                    oamBuffer[numSpritesOnScanline * 4 + m] = oamData[n * 4 + m]
                }
                numSpritesOnScanline += 1
            } else {
                statusReg.spriteOverflow = 1
            }
        }
        if (numSpritesOnScanline < 8) {
            // Since on real hardware the evaluation works by copying the y position of the sprite
            // into the secondary OAM and then evaluating, if the secondary OAM is not full,
            // the y position of the last sprite in the OAM should be the last non 0xFF value
            oamBuffer[numSpritesOnScanline * 4] = oamData[63 * 4]
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
            onReadExternalMemory(address)
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
            onWriteExternalMemory(address, value)
        }
    }

    // https://www.nesdev.org/wiki/PPU_registers
    // https://www.nesdev.org/wiki/PPU_scrolling#Register_controls
    fun cpuReadRegister(address: Int): Int {
        return when (address) {
            statusReg.address -> {
                w = false
                busLatch = (statusReg.value and 0xE0) or (busLatch and 0x1F)
                statusReg.vblank = 0
                busLatch
            }
            oamDataReg.address -> {
                busLatch = if (oamClear) 0xFF else oamData[oamAddressReg.value]
                busLatch
            }
            dataReg.address -> {
                var data = dataReg.value
                dataReg.value = readMemory(v)
                // Palette reads return values in the same cycle
                if (v >= 0x3F00) {
                    data = dataReg.value
                }
                v += if (controlReg.vramAddrIncrement > 0) 32 else 1
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
            controlReg.address -> {
                controlReg.value = value
                // Transfer the nametable select bytes from Control into the temporary address
                t = (t and 0x73FF) or (controlReg.nametableSelect shl 10)
                if (controlReg.enableVBlankNmi > 0 && statusReg.vblank > 0) {
                    // TODO: should generate NMI here, but it breaks Mario Bros
                    // https://www.nesdev.org/wiki/PPU_registers#Vblank_NMI
                    //onGenerateNMI()
                }
            }
            maskReg.address -> maskReg.value = valueByte
            oamAddressReg.address -> oamAddressReg.value = valueByte
            oamDataReg.address -> {
                oamData[oamAddressReg.value] = valueByte
                oamAddressReg.value = oamAddressReg.value.plus8(1)
            }
            scrollReg.address -> {
                if (!w) {
                    t = (t and 0x7FE0) or ((valueByte and 0xF8) ushr 3)
                    fineX = valueByte and 0x07
                } else {
                    t = (t and 0x8C1F) or ((valueByte and 0x07) shl 12) or
                            ((valueByte and 0xF8) shl 2)
                }
                w = !w
            }
            addressReg.address -> {
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
            dataReg.address -> {
                writeMemory(v, valueByte)
                v += if (controlReg.vramAddrIncrement > 0) 32 else 1
            }
        }
    }

    fun loadOamData(data: IntArray) {
        oamData = data.copyOf()
    }

    override fun captureState(): PpuState {
        return PpuState(
            controlRegister = controlReg.value,
            maskRegister = maskReg.value,
            statusRegister = statusReg.value,
            oamAddressRegister = oamAddressReg.value,
            oamDataRegister = oamDataReg.value,
            scrollRegister = scrollReg.value,
            addressRegister = addressReg.value,
            dataRegister = dataReg.value,
            busLatch = busLatch,
            palette = palette,
            v = v,
            t = t,
            fineX = fineX,
            w = w,
            cycle = cycle,
            scanline = scanline,
            oddFrame = oddFrame,
            nametableId = nametableId,
            attributeId = attributeId,
            bgTilePatternLow = bgTilePatternLow,
            bgTilePatternHigh = bgTilePatternHigh,
            bgPatternDataLow = bgPatternDataLow,
            bgPatternDataHigh = bgPatternDataHigh,
            bgAttributeDataLow = bgAttributeDataLow,
            bgAttributeDataHigh = bgAttributeDataHigh,
            oamBuffer = oamBuffer,
            numSpritesOnScanline = numSpritesOnScanline,
            oamData = oamData,
            oamClear = oamClear,
            sprPatternDataLow = sprPatternDataLow,
            sprPatternDataHigh = sprPatternDataHigh
        )
    }

    override fun loadState(state: PpuState) {
        controlReg.value = state.controlRegister
        maskReg.value = state.maskRegister
        statusReg.value = state.statusRegister
        oamAddressReg.value = state.oamAddressRegister
        oamDataReg.value = state.oamDataRegister
        scrollReg.value = state.scrollRegister
        addressReg.value = state.addressRegister
        dataReg.value = state.dataRegister
        busLatch = state.busLatch
        palette = state.palette
        v = state.v
        t = state.t
        fineX = state.fineX
        w = state.w
        cycle = state.cycle
        scanline = state.scanline
        oddFrame = state.oddFrame
        nametableId = state.nametableId
        attributeId = state.attributeId
        bgTilePatternLow = state.bgTilePatternLow
        bgTilePatternHigh = state.bgTilePatternHigh
        bgPatternDataLow = state.bgPatternDataLow
        bgPatternDataHigh = state.bgPatternDataHigh
        bgAttributeDataLow = state.bgAttributeDataLow
        bgAttributeDataHigh = state.bgAttributeDataHigh
        oamBuffer = state.oamBuffer
        numSpritesOnScanline = state.numSpritesOnScanline
        oamData = state.oamData
        oamClear = state.oamClear
        sprPatternDataLow = state.sprPatternDataLow
        sprPatternDataHigh = state.sprPatternDataHigh
    }

    // Functions used for debugging

    fun dbgCpuReadRegister(address: Int): Int {
        return when (address) {
            statusReg.address -> statusReg.value
            oamDataReg.address -> oamData[oamAddressReg.value]
            dataReg.address -> dataReg.value
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
            val tileAddress = tileIndex * TILE_BYTES + 0x1000 * controlReg.bgPatternTableSelect

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