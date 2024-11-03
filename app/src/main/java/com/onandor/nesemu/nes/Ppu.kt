package com.onandor.nesemu.nes

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
    private val readMemory: (address: Int) -> Int,
    private val writeMemory: (address: Int, value: Int) -> Unit,
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
        var nametableAddrLow: Int
            get() { return register and 0x01 }
            set(value) { register = if (value > 0) register or 0x01 else register and 0x01.inv() }
        var nametableAddrHigh: Int
            get() { return (register and 0x02) shr 1 }
            set(value) { register = if (value > 0) register or 0x02 else register and 0x02.inv() }
        var vramAddrIncrement: Int
            get() { return (register and 0x04) shr 2 }
            set(value) { register = if (value > 0) register or 0x04 else register and 0x04.inv() }
        var spritePatternTableAddr: Int
            get() { return (register and 0x08) shr 3 }
            set(value) { register = if (value > 0) register or 0x08 else register and 0x08.inv() }
        var backgroundPatternTableAddr: Int
            get() { return (register and 0x10) shr 4 }
            set(value) { register = if (value > 0) register or 0x10 else register and 0x10.inv() }
        var spriteSize: Int
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
    private var framePalette: IntArray = IntArray(32)

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
    private var x: Int = 0  // 3 bits, holds the 3 bit X scroll position within a 8x8-pixel tile
    private var w: Boolean = false  // 1 bit flag, first or second write toggle for PPUSCROLL and
                                    // PPUADDR

    private var cycle: Int = 0
    private var scanline: Int = 261 // Scanline 261 is the pre-render scanline
    private var numFrames: Int = -1 // Pre-render frame

    var mirroring: Mirroring = Mirroring.HORIZONTAL

    private var nametableByte: Int = 0
    private var attributeTableByte: Int = 0

    // https://www.nesdev.org/wiki/PPU_pattern_tables
    // Tiles are 8x8 pixels
    // These two combined store the color indices of a row from a tile being rendered during a scanline
    private var patternTableTileLow: Int = 0    // First bit plane
    private var patternTableTileHigh: Int = 0   // Second bit plane

    lateinit var frame: IntArray
        private set
    private var frameBuffer: IntBuffer = IntBuffer.allocate(SCREEN_WIDTH * SCREEN_HEIGHT)
    private var prefetchedTiles: IntBuffer = IntBuffer.allocate(16)

    // Debug variables
    var drawPatternTable: Boolean = false
    var patternTableFrame: IntArray = IntArray(128 * 256)
        private set

    var drawNametable: Boolean = false
    var nametableFrame: IntArray = IntArray(512 * 480)
        private set

    fun reset() {
        cycle = 0
        scanline = 261
        numFrames = -1
        v = 0
        t = 0
        x = 0
        w = false
        busLatch = 0
        Control.register = 0
        Mask.register = 0
        Status.register = 0
        Scroll.register = 0
        Data.register = 0
        OAMData.data = IntArray(256)
        frameBuffer.clear()
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
        if (scanline >= 240) {
            if (scanline == 241 && cycle == 1) {
                // Start of vertical blank
                Status.vblank = 1
                if (drawPatternTable) {
                    patternTableFrame = renderPatternTable()
                }
                if (drawNametable) {
                    nametableFrame = renderNametables()
                }
                frame = frameBuffer.array().copyOf()
                frameBuffer.clear()
                frameReady()
                if (Control.enableVBlankNmi > 0) {
                    generateNmi()
                }
            } else if (scanline == 261 && cycle == 1) {
                // End of vertical blank
                Status.vblank = 0
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

        when (cycle) {
            in 1 .. 256 -> {
                // 1-256: Fetch background tile data
                /*
                if (cycle == 1) {
                    // Load the 2 prefetched tiles
                    frame.put(prefetchedTiles)
                    prefetchedTiles.clear()
                }
                 */
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
                        val basePatternTable = 0x1000 * Control.backgroundPatternTableAddr
                        val fineY = (v ushr 12) and 0x07
                        val address = basePatternTable or (nametableByte shl 4) or fineY
                        patternTableTileLow = readMemory(address)
                    }
                    7 -> {
                        val basePatternTable = 0x1000 * Control.backgroundPatternTableAddr
                        val fineY = (v ushr 12) and 0x07
                        val address = (basePatternTable or (nametableByte shl 4) or fineY) + 8
                        patternTableTileHigh = readMemory(address)
                    }
                    0 -> {
                        for (i in 0 until 8) {
                            /*
                            if (!frame.hasRemaining()) {
                                println("frame full, scanline: $scanline")
                                break
                            }
                             */
                            val high = (patternTableTileHigh and (0x80 shr i)) > 0
                            val low = (patternTableTileLow and (0x80 shr i)) > 0
                            if (high && low) {
                                frameBuffer.put(0xfcba03)
                            } else if (high) {
                                frameBuffer.put(0x03fc1c)
                            } else if (low) {
                                frameBuffer.put(0x0373fc)
                            } else {
                                frameBuffer.put(0)
                            }
                        }
                    }
                }
            }
            in 321 .. 336 -> {
                // 321-336: Prefetch data for the first 2 background tiles of the next scanline
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
                        val basePatternTable = 0x1000 * Control.backgroundPatternTableAddr
                        val fineY = (v ushr 12) and 0x07
                        val address = basePatternTable or (nametableByte shl 4) or fineY
                        patternTableTileLow = readMemory(address)
                    }
                    7 -> {
                        val basePatternTable = 0x1000 * Control.backgroundPatternTableAddr
                        val fineY = (v ushr 12) and 0x07
                        val address = (basePatternTable or (nametableByte shl 4) or fineY) + 8
                        patternTableTileHigh = readMemory(address)
                    }
                    /*
                    0 -> {
                        for (i in 0 until 8) {
                            val high = (patternTableTileHigh and (0x80 shr i)) > 0
                            val low = (patternTableTileLow and (0x80 shr i)) > 0
                            if (high && low) {
                                prefetchedTiles.put(0xfcba03)
                            } else if (high) {
                                prefetchedTiles.put(0x03fc1c)
                            } else if (low) {
                                prefetchedTiles.put(0x0373fc)
                            } else {
                                prefetchedTiles.put(0)
                            }
                        }
                    }
                     */
                }
            }
            in 257 .. 320 -> {
                // Prefetch tile data for the sprites on the next scanline
                when (cycle % 8) {
                    1, 3 -> {
                        // Garbage nametable byte reads
                        nametableByte = readMemory(0x2000 or (v and 0x0FFF))
                    }
                    5 -> {
                        val basePatternTable = 0x1000 * Control.backgroundPatternTableAddr
                        val fineY = (v ushr 12) and 0x07
                        val address = basePatternTable or (nametableByte shl 4) or fineY
                        patternTableTileLow = readMemory(address)
                    }
                    7 -> {
                        val basePatternTable = 0x1000 * Control.backgroundPatternTableAddr
                        val fineY = (v ushr 12) and 0x07
                        val address = (basePatternTable or (nametableByte shl 4) or fineY) + 8
                        patternTableTileHigh = readMemory(address)
                    }
                }
            }
            in 337 ..340 -> {
                // Garbage nametable reads
                if (cycle % 8 == 1 || cycle % 8 == 3) {
                    nametableByte = readMemory(0x2000 or (v and 0x0FFF))
                }
            }
        }

        if (Mask.spriteRenderingOn + Mask.backgroundRenderingOn > 0) {
            scroll()
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

    fun tick2() {
        cycle++
        if (cycle == 340) {
            cycle = 0
            scanline++
        }

        if (scanline in 0 until 240) {
            drawBackground()
            if (Mask.spriteRenderingOn + Mask.backgroundRenderingOn > 0) {
                scroll()
            }
        } else if (scanline == 241 && cycle == 1) {
            // VBlank start
            Status.vblank = 1
            frame = frameBuffer.array().copyOf()
            frameBuffer.clear()
            frameReady()
            if (Control.enableVBlankNmi > 0) {
                generateNmi()
            }
        } else if (scanline == 261 && cycle == 1) {
            // VBlank end
            Status.vblank = 0
            scanline = 0
            numFrames++
        }
    }

    private fun drawBackground() {
        when (cycle) {
            in 1 .. 256 -> {
                // 1-256: Fetch background tile data
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
                        val basePatternTable = 0x1000 * Control.backgroundPatternTableAddr
                        val fineY = (v ushr 12) and 0b111
                        val address = basePatternTable + (nametableByte shl 4) + fineY
                        patternTableTileLow = readMemory(address)
                    }
                    7 -> {
                        val basePatternTable = 0x1000 * Control.backgroundPatternTableAddr
                        val fineY = (v ushr 12) and 0b111
                        val address = basePatternTable + (nametableByte shl 4) + fineY + 8
                        patternTableTileHigh = readMemory(address)
                    }
                    0 -> {
                        for (i in 0 until 8) {
                            val high = (patternTableTileHigh and (0x80 shr i)) > 0
                            val low = (patternTableTileLow and (0x80 shr i)) > 0
                            if (high && low) {
                                frameBuffer.put(0xfcba03)
                            } else if (high) {
                                frameBuffer.put(0x03fc1c)
                            } else if (low) {
                                frameBuffer.put(0x0373fc)
                            } else {
                                frameBuffer.put(0)
                            }
                        }
                    }
                }
            }
        }
    }

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
            // Copy all bits related to horizontal position from t into v
            v = (v and 0xFBE0) or (t and 0x041F)
        }

        // Copy remaining bits from t into v
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
                busLatch = OAMData.data[OAMAddress.register]
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
                t = (t and 0x73FF) or ((Control.register and 0b11) shl 10)
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
                    x = valueByte and 0x07
                } else {
                    t = (t and 0x73E0) or ((valueByte and 0x07) shl 12) or
                            ((valueByte and 0xF8) shl 2)
                }
                w = !w
            }
            Address.ADDRESS -> {
                if (!w) {
                    // The first write sets the high byte of the temporary address register
                    t = (t and 0xFF) or ((valueByte and 0x3F) shl 8)
                } else {
                    // The seconds write sets the low byte and transfers the value into the address
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

    fun cpuReadRegister_dbg(address: Int): Int {
        return when (address) {
            Status.ADDRESS -> Status.register
            OAMData.ADDRESS -> OAMData.data[OAMAddress.register]
            Data.ADDRESS -> Data.register
            else -> busLatch
        }
    }

    private fun renderPatternTable(): IntArray {
        val patternTable = IntArray(256 * 128)
        // Left grid (0x0000 - 0x0FFF)
        for (tileRow in 0 until GRID_SIZE) {
            for (tileCol in 0 until GRID_SIZE) {
                renderPatternTableTile(patternTable, tileRow, tileCol, 0x0000)
            }
        }
        // Right grid (0x1000 - 0x1FFF)
        for (tileRow in 0 until GRID_SIZE) {
            for (tileCol in 0 until GRID_SIZE) {
                renderPatternTableTile(patternTable, tileRow, tileCol + GRID_SIZE, 0x1000)
            }
        }
        return patternTable
    }

    private fun renderPatternTableTile(
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
                grid[pixelIdx] = COLOR_PALETTE[pixel]
            }
        }
    }

    private fun renderNametables(): IntArray {
        val frame = IntArray(512 * 480)
        renderNametable(frame, 0, 0, 0)     // Nametable 1 - Top left
        renderNametable(frame, 1, 0, 256)   // Nametable 2 - Top right
        renderNametable(frame, 2, 240, 0)   // Nametable 3 - Bottom left
        renderNametable(frame, 3, 240, 256) // Nametable 4 - Bottom right
        return frame
    }

    private fun renderNametable(frame: IntArray, nametableIdx: Int, rowOffset: Int, colOffset: Int) {
        val baseAddress = 0x2000 + nametableIdx * 0x400
        for (tileIdx in 0 until 960) {
            val tileIndex = readMemory(baseAddress + tileIdx)
            val tileAddress = tileIndex * TILE_BYTES + 0x1000 * Control.backgroundPatternTableAddr

            val tileRow = tileIdx / 32
            val tileCol = tileIdx % 32
            renderNametableTile(frame, tileAddress, tileRow, tileCol, rowOffset, colOffset)
        }
    }

    private fun renderNametableTile(
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
                frame[pixelIdx] = COLOR_PALETTE[pixel]
            }
        }
    }
}