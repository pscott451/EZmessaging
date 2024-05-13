package com.scott.ezmessaging.model

import android.graphics.Bitmap

sealed interface MessageData {

    data class Text(
        val text: String
    ): MessageData

    data class Image(
        val bitmap: Bitmap,
        val mimeType: String
    ): MessageData
}