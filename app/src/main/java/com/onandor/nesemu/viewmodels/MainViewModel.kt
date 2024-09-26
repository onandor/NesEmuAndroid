package com.onandor.nesemu.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.Nes
import com.onandor.nesemu.nes.NesException
import com.onandor.nesemu.nes.RomParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navManager: NavigationManager
) : ViewModel() {

    private val nes: Nes = Nes()
    private var cartridge: Cartridge = Cartridge()

    fun onRomSelected(stream: InputStream) {
        val rom = stream.readBytes()
        stream.close()

        try {
            cartridge.parseRom(rom)
        } catch (e: RomParseException) {
            Log.e(e.tag, e.message.toString())
            // TODO: display some kind of error message
        }

        nes.insertCartridge(cartridge)

        try {
            nes.reset()
        } catch (e: Exception) {
            if (e is NesException) {
                handleNesException(e)
            }
        }
    }

    fun handleNesException(e: NesException) {
        Log.e(e.tag, e.message.toString())
        // TODO: display some kind of error message
    }
}