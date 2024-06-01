package com.scott.ezmessaging.extension

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.scott.ezmessaging.model.Message

/**
 * @return a bitmap image if it exists. Otherwise, null
 */
fun Message.MmsMessage.getImageAsBitmap(contentResolver: ContentResolver?): Bitmap? {
    try {
        val inputStream = contentResolver?.openInputStream(getLocationUri())
        BitmapFactory.decodeStream(inputStream).apply {
            inputStream?.close()
            return this
        }
    } catch (e: Exception) {
        return null
    }
}


/**
 * @return the URI where the image is located.
 */
fun Message.MmsMessage.getLocationUri(): Uri = Uri.parse("content://mms/part/$uniqueId")