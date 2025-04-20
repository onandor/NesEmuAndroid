package com.onandor.nesemu.domain.emulation.savestate

import kotlinx.serialization.Serializable

@Serializable
data class PpuState(
    val controlRegister: Int,
    val maskRegister: Int,
    val statusRegister: Int,
    val oamAddressRegister: Int,
    val oamDataRegister: Int,
    val scrollRegister: Int,
    val addressRegister: Int,
    val dataRegister: Int,
    val busLatch: Int,
    val palette: IntArray,
    val v: Int,
    val t: Int,
    val fineX: Int,
    val w: Boolean,
    val cycle: Int,
    val scanline: Int,
    val oddFrame: Boolean,
    val nametableId: Int,
    val attributeId: Int,
    val bgTilePatternLow: Int,
    val bgTilePatternHigh: Int,
    val bgPatternDataLow: Int,
    val bgPatternDataHigh: Int,
    val bgAttributeDataLow: Int,
    val bgAttributeDataHigh: Int,
    val oamBuffer: IntArray,
    val numSpritesOnScanline: Int,
    val oamData: IntArray,
    val oamClear: Boolean,
    val sprPatternDataLow: IntArray,
    val sprPatternDataHigh: IntArray
) : SaveState()