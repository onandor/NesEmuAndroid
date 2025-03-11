package com.onandor.nesemu.preferences

import androidx.datastore.core.DataStore
import com.onandor.nesemu.preferences.proto.InputDevicePref
import com.onandor.nesemu.preferences.proto.InputPreferences
import com.onandor.nesemu.preferences.proto.Preferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProtoPreferenceStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun observe() = dataStore.data.catch { emit(Preferences.getDefaultInstance()) }

    suspend fun get() = observe().first()

    suspend fun updateControllerDevices(device1: InputDevicePref?, device2: InputDevicePref?) {
        dataStore.updateData { prefs ->
            prefs.toBuilder()
                .setInputPreferences(
                    setInputDevicePreferences(
                        prefs.inputPreferences,
                        device1,
                        device2
                    )
                ).build()
        }
    }

    private fun setInputDevicePreferences(
        inputPreferences: InputPreferences,
        device1: InputDevicePref?,
        device2: InputDevicePref?
    ): InputPreferences {
        return inputPreferences.toBuilder().run {
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