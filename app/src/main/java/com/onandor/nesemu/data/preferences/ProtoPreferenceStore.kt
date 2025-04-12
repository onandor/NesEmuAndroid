package com.onandor.nesemu.data.preferences

import com.onandor.nesemu.data.preferences.proto.InputDevicePref
import com.onandor.nesemu.data.preferences.proto.Preferences
import kotlinx.coroutines.flow.Flow

interface ProtoPreferenceStore {

    fun observe(): Flow<Preferences>
    suspend fun updateUseDarkTheme(useDarkTheme: Boolean)
    suspend fun updateLibraryUri(libraryUri: String)
    suspend fun updateSteamGridDBApiKey(apiKey: String)
    suspend fun updateInputDevices(device1: InputDevicePref?, device2: InputDevicePref?)
    suspend fun updateButtonMappings(
        player1ControllerMapping: Map<Int, Int>,
        player1KeyboardMapping: Map<Int, Int>,
        player2ControllerMapping: Map<Int, Int>,
        player2KeyboardMapping: Map<Int, Int>
    )
}