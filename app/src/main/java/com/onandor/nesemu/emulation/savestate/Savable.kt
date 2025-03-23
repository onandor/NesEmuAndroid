package com.onandor.nesemu.emulation.savestate

abstract class SaveState

interface Savable<T : SaveState> {

    fun createSaveState(): T
    fun loadState(state: T)
}