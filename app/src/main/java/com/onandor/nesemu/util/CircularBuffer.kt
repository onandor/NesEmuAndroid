package com.onandor.nesemu.util

class CircularBuffer<T>(val size: Int) {

    private var buffer = arrayOfNulls<Any?>(size)
    private var head: Int = 0
    private var tail: Int = 0

    fun add(element: T?) {
        buffer[head++ % size] = element
        if (head - tail > size) {
            tail += 1
        }
    }

    fun clear() {
        buffer = arrayOfNulls<Any?>(size)
        head = 0
        tail = 0
    }

    @Suppress("UNCHECKED_CAST")
    fun getAll(): Array<T?> {
       var out = arrayOfNulls<Any?>(head - tail)
       var i = 0
       while (tail != head - 1) {
           out[i++] = buffer[tail++]
       }
       return out as Array<T?>
    }
}