package com.onandor.nesemu.domain.emulation.savestate

abstract class SaveState

interface Savable<T : SaveState> {

    fun captureState(): T
    fun loadState(state: T)
}