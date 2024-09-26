package com.onandor.nesemu.nes

sealed interface NesEvent {

    sealed interface CartridgeEvent : NesEvent {
        data class MapperNotSupported(val mapperID: Int): CartridgeEvent
        object ChrRamNotSupported: CartridgeEvent
        object InvalidRom: CartridgeEvent
        data class Ready: CartridgeEvent
    }
}
