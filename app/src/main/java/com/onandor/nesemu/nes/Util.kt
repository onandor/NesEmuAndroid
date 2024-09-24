package com.onandor.nesemu.nes

typealias U16 = UShort
typealias U8 = Byte
typealias U32 = UInt

fun U8.shl(bitCount: Int): U8 = this.toUInt().shl(bitCount).toByte()
fun U8.shr(bitCount: Int): U8 = this.toUInt().shr(bitCount).toByte()
