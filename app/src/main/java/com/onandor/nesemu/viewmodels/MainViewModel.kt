package com.onandor.nesemu.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.nesemu.navigation.NavigationManager
import com.onandor.nesemu.nes.Cartridge
import com.onandor.nesemu.nes.Bus
import com.onandor.nesemu.nes.NesEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navManager: NavigationManager
) : ViewModel() {

    private val bus: Bus = Bus()
    private var cartridge: Cartridge = Cartridge()

    init {
        viewModelScope.launch { bus.eventFlow.collect(::onBusEvent) }
    }

    fun onRomSelected(stream: InputStream) {
        val rom = stream.readBytes()
        stream.close()
        cartridge.parseRom(rom)
    }

    fun onBusEvent(event: NesEvent) {
        when (event) {
            is NesEvent.CartridgeEvent -> onCartridgeEvent(event)
        }
    }

    fun onCartridgeEvent(event: NesEvent.CartridgeEvent) {
        when (event) {
            is NesEvent.CartridgeEvent.Ready -> bus.insertCartridge(cartridge)
            NesEvent.CartridgeEvent.ChrRamNotSupported -> { println("ChrRamNotSupported") }
            NesEvent.CartridgeEvent.InvalidRom -> { println("InvalidRom") }
            is NesEvent.CartridgeEvent.MapperNotSupported -> { println("MapperNotSupported") }
        }
    }
}