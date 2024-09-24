package com.onandor.nesemu.nes

class Memory {

    companion object {
        const val SIZE = 655536 // 64 KB
    }

    val data: IntArray = IntArray(SIZE)

    operator fun get(address: Int) = data[address.toInt()]
    operator fun set(address: Int, value: Int) {
        data[address.toInt()] = value
    }
}