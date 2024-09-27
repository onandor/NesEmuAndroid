package com.onandor.nesemu.nes

fun Boolean.toInt() = if (this) 1 else 0
fun Int.plus8(value: Int): Int = (this + value) and 0xFF
fun Int.plus16(value: Int): Int = (this + value) and 0xFFFF
fun Int.toSigned8(): Int = this.toUByte().toByte().toInt()

fun ByteArray.toByteIntArray(): IntArray {
    return this.foldIndexed(IntArray(this.size)) { index, intArray, value ->
        intArray.apply { set(index, value.toInt() and 0xFF) }
    }
}

fun Int.toHexString(length: Int, padChar: Char = '0', prefix: String = ""): String =
    prefix + this.toString(16).padStart(length, padChar).uppercase()
