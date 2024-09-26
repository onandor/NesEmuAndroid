package com.onandor.nesemu.nes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class Bus {

    private companion object {
        const val MEMORY_SIZE = 2048
    }

    private var memory: IntArray = IntArray(MEMORY_SIZE)
    private val cpu: Cpu = Cpu(::readMemory, ::writeMemory)
    private var cartridge: Cartridge? = null

    private val mEventFlow = MutableSharedFlow<NesEvent>()
    val eventFlow = mEventFlow.asSharedFlow()
    private var cartridgeCollectorJob: Job? = null

    fun readMemory(address: Int): Int {
        return memory[address]
    }

    fun writeMemory(address: Int, value: Int) {
        memory[address] = value
    }

    fun insertCartridge(cartridge: Cartridge) {
        this.cartridge = cartridge
        cartridgeCollectorJob = CoroutineScope(Dispatchers.Main).launch {
            cartridge.eventFlow.collect(::onCartridgeEvent)
        }
    }

    fun reset() {
        cpu.reset()
    }

    fun onCartridgeEvent(event: NesEvent.CartridgeEvent) {
        emitEvent(event)
    }

    fun emitEvent(event: NesEvent) {
        CoroutineScope(Dispatchers.Main).launch {
            mEventFlow.emit(event)
        }
    }
}