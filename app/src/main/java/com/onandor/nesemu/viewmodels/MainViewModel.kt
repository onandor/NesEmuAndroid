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

data class MainScreenUiState(
    val errorMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navManager: NavigationManager,
    private val emulator: Emulator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onRomSelected(stream: InputStream) {
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

    fun errorMessageToastShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}