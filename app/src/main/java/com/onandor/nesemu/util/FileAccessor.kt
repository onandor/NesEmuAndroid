package com.onandor.nesemu.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileAccessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getFileName(uriString: String): String? {
        val uri = Uri.parse(uriString)
        val cursor = context.contentResolver.query(uri, null, null, null, null, null)
        cursor?.use {
            if (!it.moveToFirst()) {
                return null
            }
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                return it.getString(nameIndex)
            }
        }
        return null
    }

    fun readBytes(uriString: String): ByteArray {
        val stream = context.contentResolver.openInputStream(Uri.parse(uriString))
            ?: throw RuntimeException("Unable to open input stream")
        return stream.readBytes().also { stream.close() }
    }
}