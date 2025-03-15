package com.onandor.nesemu.emulation.savestate

abstract class SaveState

interface Savable<T : SaveState> {

    fun saveState(): T
    fun loadState(state: T)
}