package com.onandor.nesemu.emulation.savestate

data class PpuState(
    val controlRegister: Int,
    val maskRegister: Int,
    val statusRegister: Int,
    val oamAddressRegister: Int,
    val oamDataRegister: Int,
    val scrollRegister: Int,
    val addressRegister: Int,
    val dataRegister: Int,

    val v: Int,
    val t: Int,
    val fineX: Int,
    val w: Boolean,

    val cycle: Int,
    val scanline: Int,
    val oddFrame: Boolean,
    val busLatch: Int,
    // mirroring?

    val palette: IntArray,
    val nametableId: Int,
    val attributeId: Int,
    val bgTilePatternLow: Int,
    val bgTilePatternHigh: Int,
    val bgPatternDataLow: Int,
    val bgPatternDataHigh: Int,
    val bgAttributeDataLow: Int,
    val bgAttributeDataHigh: Int,
    val sprPatternDataLow: IntArray,
    val sprPatternDataHigh: IntArray,
    val numSpritesOnScanline: Int,

    val oamBuffer: IntArray,
    val oamData: IntArray,
    val oamClear: Boolean,
    // frameBuffer?
) : SaveState()