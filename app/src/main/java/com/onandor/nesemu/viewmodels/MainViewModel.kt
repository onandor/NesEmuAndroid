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
import okio.FileNotFoundException
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
        data class OnRomSelected(val uriString: String) : Event()
        object OnErrorMessageToastShown : Event()
        object OnNavigateToPreferences : Event()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: Event) {
        when (event) {
            is Event.OnRomSelected -> {
                onRomSelected(event.uriString)
            }
            Event.OnErrorMessageToastShown -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            Event.OnNavigateToPreferences -> {
                navManager.navigateTo(NavActions.preferencesScreen())
            }
        }
    }

    private fun onRomSelected(uriString: String) {
        try {
            emulator.loadRomFile(uriString)
            navManager.navigateTo(NavActions.gameScreen())
        } catch (e: RomParseException) {
            _uiState.update { it.copy(errorMessage = e.message) }
        } catch (e: FileNotFoundException) {
            _uiState.update { it.copy(errorMessage = "The selected ROM file is missing") }
        } catch (e: Exception) {
            Log.e("MainViewModel", e.localizedMessage, e)
            _uiState.update {
                it.copy(errorMessage = "An unexpected error occurred while reading the ROM file")
            }
        }
    }
}