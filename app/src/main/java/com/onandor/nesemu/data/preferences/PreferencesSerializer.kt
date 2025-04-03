package com.onandor.nesemu.data.preferences

import androidx.datastore.core.Serializer
import com.onandor.nesemu.data.preferences.proto.Preferences
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesSerializer @Inject constructor() : Serializer<Preferences> {

    override val defaultValue: Preferences = Preferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Preferences {
        return Preferences.parseFrom(input)
    }

    override suspend fun writeTo(prefs: Preferences, output: OutputStream) {
        prefs.writeTo(output)
    }
}