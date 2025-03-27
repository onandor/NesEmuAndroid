package com.onandor.nesemu.preferences

import androidx.datastore.core.DataStore
import com.onandor.nesemu.preferences.proto.InputDevicePref
import com.onandor.nesemu.preferences.proto.InputPreferences
import com.onandor.nesemu.preferences.proto.Preferences
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

class ProtoPreferenceStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun observe() = dataStore.data.catch { emit(Preferences.getDefaultInstance()) }

    suspend fun updateLibraryUri(libraryUri: String) {
        dataStore.updateData { prefs -> prefs.toBuilder().setLibraryUri(libraryUri).build() }
    }

    suspend fun updateInputDevices(device1: InputDevicePref?, device2: InputDevicePref?) {
        dataStore.updateData { prefs ->
            prefs.toBuilder()
                .setInputPreferences(prefs.inputPreferences.setInputDevices(device1, device2))
                .build()
        }
    }

    suspend fun updateButtonMappings(
        player1ControllerMapping: Map<Int, Int>,
        player1KeyboardMapping: Map<Int, Int>,
        player2ControllerMapping: Map<Int, Int>,
        player2KeyboardMapping: Map<Int, Int>
    ) {
        dataStore.updateData { prefs ->
            prefs.toBuilder()
                .setInputPreferences(
                    prefs.inputPreferences.setButtonMappings(
                        player1ControllerMapping = player1ControllerMapping,
                        player1KeyboardMapping = player1KeyboardMapping,
                        player2ControllerMapping = player2ControllerMapping,
                        player2KeyboardMapping = player2KeyboardMapping
                    )
                )
                .build()
        }
    }

    private fun InputPreferences.setButtonMappings(
        player1ControllerMapping: Map<Int, Int>,
        player1KeyboardMapping: Map<Int, Int>,
        player2ControllerMapping: Map<Int, Int>,
        player2KeyboardMapping: Map<Int, Int>
    ): InputPreferences {
        return this.toBuilder().run {
            clearPlayer1ControllerMapping()
            clearPlayer1KeyboardMapping()
            clearPlayer2ControllerMapping()
            clearPlayer2KeyboardMapping()
            putAllPlayer1ControllerMapping(player1ControllerMapping)
            putAllPlayer1KeyboardMapping(player1KeyboardMapping)
            putAllPlayer2ControllerMapping(player2ControllerMapping)
            putAllPlayer2KeyboardMapping(player2KeyboardMapping)
            build()
        }
    }

    private fun InputPreferences.setInputDevices(
        device1: InputDevicePref?,
        device2: InputDevicePref?
    ): InputPreferences {
        return this.toBuilder().run {
            if (device1 != null) {
                setController1Device(device1)
            } else {
                clearController1Device()
            }
            if (device2 != null) {
                setController2Device(device2)
            } else {
                clearController2Device()
            }
            build()
        }
    }
}