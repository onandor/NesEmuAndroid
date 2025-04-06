package com.onandor.nesemu.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

data class Document(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val parentDirectoryUri: Uri
)

@Singleton
class DocumentAccessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun traverseDirectory(directoryUri: Uri, subdirectory: Boolean = false): List<Document> {
        val contentResolver = context.contentResolver
        val documentList = mutableListOf<Document>()

        val childrenUri = if (subdirectory) {
            DocumentsContract.buildChildDocumentsUriUsingTree(
                directoryUri,
                DocumentsContract.getDocumentId(directoryUri)
            )
        } else {
            DocumentsContract.buildChildDocumentsUriUsingTree(
                directoryUri,
                DocumentsContract.getTreeDocumentId(directoryUri)
            )
        }

        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { cursor ->
            val documentIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeTypeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val displayNameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val documentId = cursor.getString(documentIdIndex)
                val mimeType = cursor.getString(mimeTypeIndex)
                val displayName = cursor.getString(displayNameIndex)

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)

                documentList.add(
                    Document(
                        uri = documentUri,
                        name = displayName,
                        isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
                        parentDirectoryUri = directoryUri
                    )
                )

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    documentList.addAll(traverseDirectory(documentUri, true))
                }
            }
        }

        return documentList
    }

    fun getFileName(uriString: String): String? {
        val uri = uriString.toUri()
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

    fun getDocumentName(uriString: String): String? =
        DocumentsContract.getTreeDocumentId(uriString.toUri()).split(":").lastOrNull()

    fun readBytes(uriString: String): ByteArray {
        val stream = context.contentResolver.openInputStream(uriString.toUri())
            ?: throw RuntimeException("Unable to open input stream")
        return stream.readBytes().also { stream.close() }
    }

    companion object {
        private fun isDirectory(documentUri: Uri) {

        }
    }
}