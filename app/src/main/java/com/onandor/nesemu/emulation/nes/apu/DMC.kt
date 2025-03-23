package com.onandor.nesemu.emulation.nes.apu

import com.onandor.nesemu.emulation.savestate.DMCOutputState
import com.onandor.nesemu.emulation.savestate.DMCSampleState
import com.onandor.nesemu.emulation.savestate.DMCState
import com.onandor.nesemu.emulation.savestate.Savable

class DMC(
    private val onGenerateIRQ: () -> Unit,
    private val onReadMemory: (address: Int) -> Int
) : Savable<DMCState> {

    class Sample : Savable<DMCSampleState> {
        var address: Int = 0            // Address of the next byte to be loaded
        var startingAddress: Int = 0    // Starting address of the sample
        var length: Int = 0             // Length of the sample in bytes
        var bytesRemaining: Int = 0     // Bytes remaining from the sample
        var buffer: Int = 0             // 1 byte buffer for the next byte of the sample
        var isEmpty: Boolean = true

        fun reset() {
            address = 0
            startingAddress = 0
            length = 0
            bytesRemaining = 0
            buffer = 0
            isEmpty = true
        }

        override fun createSaveState(): DMCSampleState {
            return DMCSampleState(
                address = address,
                startingAddress = startingAddress,
                length = length,
                bytesRemaining = bytesRemaining,
                buffer = buffer,
                isEmpty = isEmpty
            )
        }

        override fun loadState(state: DMCSampleState) {
            address = state.address
            startingAddress = state.startingAddress
            length = state.length
            bytesRemaining = state.bytesRemaining
            buffer = state.buffer
            isEmpty = state.isEmpty
        }
    }

    class Output : Savable<DMCOutputState> {
        var bitsRemaining: Int = 0          // Bits remaining in the shifter
        var level: Int = 0                  // Output level, controlled by the shifter
        var isSilenced: Boolean = true      // Set if a new sample byte cannot be loaded from the reader
        var shifter: Int = 0                // 8 bit right-shifter that shifts trough the loaded sample

        fun reset() {
            bitsRemaining = 0
            level = 0
            isSilenced = true
            shifter = 0
        }

        override fun createSaveState(): DMCOutputState {
            return DMCOutputState(
                bitsRemaining = bitsRemaining,
                level = level,
                isSilenced = isSilenced,
                shifter = shifter
            )
        }

        override fun loadState(state: DMCOutputState) {
            bitsRemaining = state.bitsRemaining
            level = state.level
            isSilenced = state.isSilenced
            shifter = state.shifter
        }
    }

    var interruptEnable: Boolean = false
    var isLooping: Boolean = false
    var isEnabled: Boolean = false

    var sample = Sample()
    var output = Output()

    var divider = Divider {
        clockReader()
        clockOutput()
    }

    private fun clockReader() {
        if (!sample.isEmpty || !isEnabled) {
            return
        }

        // Load the next byte of the sample
        sample.buffer = onReadMemory(sample.address)
        sample.address += 1
        if (sample.address > 0xFFFF) {
            sample.address = 0x8000
        }
        sample.bytesRemaining -= 1
        sample.isEmpty = false

        if (sample.bytesRemaining == 0) {
            if (isLooping) {
                // If looping, reload the sample
                sample.address = sample.startingAddress
                sample.bytesRemaining = sample.length
            } else if (interruptEnable) {
                // TODO: set interrupt flag, generate IRQ
            }
        }
    }

    private fun clockOutput() {
        if (output.bitsRemaining == 0) {
            output.bitsRemaining = 8
            if (sample.isEmpty) {
                output.isSilenced = true
            } else {
                // Dump next sample into shift register
                output.isSilenced = false
                output.shifter = sample.buffer
                sample.isEmpty = true
            }
        } else {
            if (!output.isSilenced) {
                val change = if (output.shifter and 0x01 > 0) 2 else -2
                output.level = (output.level + change).coerceIn(0, 127)
                output.shifter = output.shifter ushr 1
            }
            output.bitsRemaining -= 1
        }
    }

    fun reset() {
        interruptEnable = false
        isLooping = false
        isEnabled = false

        sample.reset()
        output.reset()
        divider.reset()
    }

    fun setControl(value: Int) {
        interruptEnable = value and 0x80 > 0
        isLooping = value and 0x40 > 0
        divider.period = RATE_LOOKUP[value and 0x0F]
    }

    fun setDirectLoad(value: Int) {
        output.level = value and 0x7F
    }

    fun setSampleAddress(value: Int) {
        sample.startingAddress = 0xC000 + (value shl 6)
    }

    fun setSampleLength(value: Int) {
        sample.length = (value shl 4) + 1
    }

    fun getOutput(): Int = if (output.isSilenced) 0 else output.level

    override fun createSaveState(): DMCState {
        return DMCState(
            interruptEnable = interruptEnable,
            isLooping = isLooping,
            isEnabled = isEnabled,
            sample = sample.createSaveState(),
            output = output.createSaveState(),
            divider = divider.createSaveState()
        )
    }

    override fun loadState(state: DMCState) {
        interruptEnable = state.interruptEnable
        isLooping = state.isLooping
        isEnabled = state.isEnabled
        sample.loadState(state.sample)
        output.loadState(state.output)
        divider.loadState(state.divider)
    }

    companion object {
        private val RATE_LOOKUP: IntArray = intArrayOf(
            428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54
        )
    }
}