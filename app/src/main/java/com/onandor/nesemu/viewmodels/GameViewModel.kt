package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.navigation.CartridgeNavArgs
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.Nes
import com.onandor.nesemu.nes.NesException
import jakarta.inject.Inject

class GameViewModel @Inject constructor(
    navManager: NavigationManager
) : ViewModel() {

    private val nes: Nes = Nes()

    init {
        val cartridge = (navManager.getCurrentNavAction()!!.navArgs as CartridgeNavArgs).cartridge
        nes.insertCartridge(cartridge)

        try {
            nes.reset()
        } catch (e: Exception) {
            if (e is NesException) {
                Log.e(e.tag, e.message.toString())
                // TODO: display some kind of error message
            }
        }
    }
}