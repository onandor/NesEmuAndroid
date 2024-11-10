package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.navigation.CartridgeNavArgs
import com.onandor.nesemu.navigation.NavActions
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.RomParseException
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
    private val navManager: NavigationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onRomSelected(stream: InputStream) {
        val cartridge: Cartridge = Cartridge()
        val rom = stream.readBytes()
        stream.close()

        try {
            cartridge.parseRom(rom)
        } catch (e: RomParseException) {
            Log.e(e.tag, e.message.toString())
            _uiState.update { it.copy(errorMessage = e.message) }
        }

        navManager.navigateTo(NavActions.gameScreen(CartridgeNavArgs(cartridge)))
    }

    fun errorMessageToastShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}