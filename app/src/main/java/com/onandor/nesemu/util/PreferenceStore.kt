package com.onandor.nesemu.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

object Preferences {
    object Input {
        val CONTROLLER_1_DESCRIPTOR = stringPreferencesKey("input.controller_1_descriptor")
        val CONTROLLER_2_DESCRIPTOR = stringPreferencesKey("input.controller_2_descriptor")
    }
}

class PreferenceStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun <T> observe(key: Preferences.Key<T>, missingValue: T): Flow<T> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs[key] ?: missingValue }
    }

    suspend fun <T> get(key: Preferences.Key<T>, missingValue: T): T {
        return observe(key, missingValue).first()
    }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit { prefs -> prefs.remove(key) }
    }
}