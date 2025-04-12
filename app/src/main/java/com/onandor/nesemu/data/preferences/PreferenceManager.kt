package com.onandor.nesemu.data.preferences

import com.google.common.collect.BiMap
import com.onandor.nesemu.domain.input.NesButton
import com.onandor.nesemu.domain.input.NesInputDevice
import com.onandor.nesemu.domain.service.InputService.ButtonMapKey
import kotlinx.coroutines.flow.Flow

interface PreferenceManager {

    fun observeUseDarkTheme(): Flow<Boolean>
    fun observeLibraryUri(): Flow<String>
    suspend fun getLibraryUri(): String
    fun observeSteamGridDBApiKey(): Flow<String>
    suspend fun getController1Device(): NesInputDevice?
    suspend fun getController2Device(): NesInputDevice?
    suspend fun getButtonMappings(): Map<ButtonMapKey, BiMap<Int, NesButton>>
    suspend fun updateUseDarkTheme(useDarkTheme: Boolean)
    suspend fun updateLibraryUri(libraryUri: String)
    suspend fun updateSteamGridDBApiKey(apiKey: String)
    suspend fun updateInputDevices(device1: NesInputDevice?, device2: NesInputDevice?)
    suspend fun updateButtonMappings(mappings: Map<ButtonMapKey, BiMap<Int, NesButton>>)
}