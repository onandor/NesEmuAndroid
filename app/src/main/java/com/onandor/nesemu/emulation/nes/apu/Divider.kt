package com.onandor.nesemu.emulation.nes.apu

import com.onandor.nesemu.emulation.savestate.DividerState
import com.onandor.nesemu.emulation.savestate.Savable

// https://www.nesdev.org/wiki/APU#Glossary
class Divider(private val outClock: () -> Unit) : Clockable, Savable<DividerState> {
    var counter: Int = 0
    var period: Int = 0

    override fun reset() {
        counter = 0
        period = 0
    }

    fun reload() {
        counter = period + 1
    }

    override fun clock() {
        if (counter == 0) {
            if (period != 0) {
                reload()
                outClock()
            }
        } else {
            counter -= 1
        }
    }

    override fun createSaveState(): DividerState {
        return DividerState(
            counter = counter,
            period = period
        )
    }

    override fun loadState(state: DividerState) {
        counter = state.counter
        period = state.period
    }
}