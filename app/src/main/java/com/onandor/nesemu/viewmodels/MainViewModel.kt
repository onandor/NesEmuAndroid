package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.emulation.Emulator
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.emulation.nes.RomParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val emulator: Emulator
) : ViewModel() {

    data class UiState(
        val errorMessage: String? = null
    )

    sealed class Event {
        data class OnRomSelected(val inputStream: InputStream) : Event()
        object OnErrorMessageToastShown : Event()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnRomSelected -> {
                onRomSelected(event.inputStream)
            }
            Event.OnErrorMessageToastShown -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun onRomSelected(stream: InputStream) {
        val rom = stream.readBytes()
        stream.close()

        try {
            emulator.parseAndInsertRom(rom)
        } catch (e: Exception) {
            if (e is RomParseException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } else {
                Log.e("MainViewModel", e.localizedMessage, e)
                _uiState.update {
                    it.copy(errorMessage = "An exception occurred while reading the ROM file")
                }
            }
            return
        }

        navManager.navigateTo(NavActions.gameScreen())
    }
}