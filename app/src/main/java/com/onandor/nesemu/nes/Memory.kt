package com.onandor.nesemu.nes

class Memory {

    companion object {
        const val SIZE = 655536 // 64 KB
    }

    val data: ByteArray = ByteArray(SIZE)

    operator fun get(address: U16) = data[address.toInt()]
    operator fun set(address: U16, value: U8) {
        data[address.toInt()] = value
    }
}