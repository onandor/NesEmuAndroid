package com.onandor.nesemu.util

import java.util.LinkedList
import java.util.Queue

class SlidingWindowIntQueue(private val windowSize: Int) {

    val values: Queue<Int> = LinkedList()
    private var total = 0

    val average: Int
        get() = if (values.isEmpty()) 0 else total / values.size

    fun add(value: Int) {
        if (values.size == windowSize) {
            total -= values.remove()
        }
        total += value
        values.add(value)
    }

    fun clear() {
        values.clear()
        total = 0
    }

    fun isFull(): Boolean {
        return values.size == windowSize
    }
}