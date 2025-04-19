package com.onandor.nesemu.domain.service

import com.onandor.nesemu.data.entity.LibraryEntry
import com.onandor.nesemu.data.entity.SaveState
import com.onandor.nesemu.domain.emulation.Emulator
import com.onandor.nesemu.domain.emulation.nes.Nes
import com.onandor.nesemu.ui.components.game.NesRenderer
import kotlinx.coroutines.flow.SharedFlow

interface EmulationService {

    val emulator: Emulator
    val loadedGame: LibraryEntry?
    val state: EmulationState
    var renderer: NesRenderer?
    val frames: SharedFlow<Nes.Frame>

    fun loadGame(game: LibraryEntry, saveState: SaveState? = null)
    fun loadSave(saveState: SaveState)
    fun saveGame(slot: Int, blocking: Boolean = false)
    fun start()
    fun stop(immediate: Boolean = false)
    fun pause()
    fun reset()
}