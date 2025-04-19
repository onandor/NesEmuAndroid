package com.onandor.nesemu.domain.emulation.nes.apu

// https://www.nesdev.org/wiki/APU_DMC
// Unlike the other channels, the DMC channel reads from the memory and then outputs delta-encoded
// audio samples.

class Dmc(private val onReadMemory: (address: Int) -> Int) {

    // The memory reader unit is responsible for filling the sample buffer byte with the next by from the currently
    // playing sample. It keeps track of the address and length of the sample.
    class Reader {
        var address: Int = 0                // Address of the next byte to be loaded
        var startingAddress: Int = 0        // Starting address of the sample
        var length: Int = 0                 // Length of the sample in bytes
        var bytesRemaining: Int = 0         // Bytes remaining from the sample
        var buffer: Int = 0                 // 1 byte buffer for the next byte of the sample
        var bufferEmpty: Boolean = true

        fun reset() {
            address = 0
            startingAddress = 0
            length = 0
            bytesRemaining = 0
            buffer = 0
            bufferEmpty = true
        }
    }

    // The output unit continuously outputs a 7 bit output level to the mixer.
    class Output {
        var bitsRemaining: Int = 0          // Bits remaining in the shifter
        var level: Int = 0                  // Output level, controlled by the shifter
        var silenced: Boolean = true        // Set if a new sample byte cannot be loaded from the reader
        var shifter: Int = 0                // 8 bit right-shifter that shifts through the loaded sample

        fun reset() {
            bitsRemaining = 0
            level = 0
            silenced = true
            shifter = 0
        }
    }

    private var enabled: Boolean = false

    // The timer contains the rate at which the output level changes (CPU cycles between level changes)
    // It is loaded from the RATE_LOOKUP table.
    private var timer: Int = 0
    private var timerPeriod: Int = 0

    private var interruptEnable: Boolean = false
    var interrupt: Boolean = false
        private set
    private var loop: Boolean = false

    val reader = Reader()
    private val output = Output()

    fun clockTimer() {
        clockReader()
        timer -= 1
        if (timer == -1) {
            timer = timerPeriod
            clockOutput()
        }
    }

    private fun clockReader() {
        if (!reader.bufferEmpty || reader.bytesRemaining == 0) {
            return
        }

        // Load the next byte of the sample
        reader.buffer = onReadMemory(reader.address)
        reader.address += 1
        if (reader.address > 0xFFFF) {
            reader.address = 0x8000
        }
        reader.bytesRemaining -= 1
        reader.bufferEmpty = false

        if (reader.bytesRemaining == 0) {
            if (loop) {
                // If looping, reload the sample
                reader.address = reader.startingAddress
                reader.bytesRemaining = reader.length
            } else if (interruptEnable) {
                interrupt = true
            }
        }
    }

    private fun clockOutput() {
        if (output.bitsRemaining == 0) {
            // Start next cycle
            output.bitsRemaining = 8
            if (reader.bufferEmpty) {
                output.silenced = true
            } else {
                // Dump next sample into shift register
                output.silenced = false
                output.shifter = reader.buffer
                reader.bufferEmpty = true
            }
        }
        if (!output.silenced) {
            val change = if (output.shifter and 0x01 > 0) 2 else -2
            output.level = (output.level + change).coerceIn(0, 127)
        }
        output.shifter = output.shifter ushr 1
        output.bitsRemaining -= 1
    }

    fun reset() {
        enabled = false
        timer = 0
        timerPeriod = RATE_LOOKUP[0]
        interruptEnable = false
        interrupt = false
        loop = false

        reader.reset()
        output.reset()
    }

    // 0x4010
    fun writeFlagsAndRate(value: Int) {
        interruptEnable = value and 0x80 > 0
        if (!interruptEnable) {
            interrupt = false
        }
        loop = value and 0x40 > 0
        timerPeriod = RATE_LOOKUP[value and 0x0F]
    }

    // 0x4011
    fun writeDirectLoad(value: Int) {
        output.level = value and 0x7F
    }

    // 0x4012
    fun writeSampleAddress(value: Int) {
        reader.startingAddress = 0xC000 + (value shl 6)
    }

    // 0x4013
    fun writeSampleLength(value: Int) {
        reader.length = (value shl 4) + 1
    }

    // 0x4015 enable bit
    fun writeEnabled(enabled: Boolean) {
        this.enabled = enabled
        interrupt = false
        if (!enabled) {
            reader.bytesRemaining = 0
        } else {
            if (reader.bytesRemaining == 0) {
                reader.address = reader.startingAddress
                reader.bytesRemaining = reader.length
            }
        }
    }

    fun getOutput(): Int = output.level

    companion object {
        private val RATE_LOOKUP: IntArray = intArrayOf(
            428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54
        )
    }
}