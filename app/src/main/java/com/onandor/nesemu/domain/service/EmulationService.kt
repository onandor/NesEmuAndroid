package com.onandor.nesemu.domain.service

import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.domain.emulation.EmulationListener
import com.onandor.nesemu.ui.components.game.NesRenderer

interface EmulationService {

    val loadedGame: LibraryEntry?
    val state: EmulationState
    var renderer: NesRenderer?

    fun loadGame(game: LibraryEntry, saveState: SaveState? = null)
    fun loadSave(saveState: SaveState)
    fun saveGame(slot: Int, blocking: Boolean = false)
    fun start()
    fun stop(immediate: Boolean = false)
    fun pause()
    fun reset()
    fun registerListener(listener: EmulationListener)
    fun unregisterListener(listener: EmulationListener)
}