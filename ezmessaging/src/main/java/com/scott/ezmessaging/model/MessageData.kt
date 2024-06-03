package com.scott.ezmessaging.model

import android.graphics.Bitmap
import android.net.Uri

sealed interface MessageData {

    data class Text(
        val text: String
    ): MessageData

    data class Image(
        val bitmap: Bitmap,
        val mimeType: String
    ): MessageData

    /**
     * @param uri the location on the device of the image.
     * (e.g. content://com.android.providers.downloads.documents/document/20)
     */
    data class ContentUri(
        val uri: Uri
    ): MessageData
}