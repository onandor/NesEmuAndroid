package com.onandor.nesemu.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.Nes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navManager: NavigationManager
) : ViewModel() {

    private val nes: Nes = Nes()

    fun onRomSelected(stream: InputStream) {
        val rom = stream.readBytes()
        stream.close()

        val cartridge = Cartridge()
        if (cartridge.parseRom(rom)) {
            nes.insertCartridge(cartridge)
        } else {
            // TODO: snackbar
        }
    }
}