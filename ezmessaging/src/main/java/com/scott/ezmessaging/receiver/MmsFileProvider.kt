package com.scott.ezmessaging.receiver

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import java.io.File
import java.io.FileNotFoundException

/**
 * A [ContentProvider] who's sole responsibility is opening files where received mms files reside after downloading them.
 */
internal class MmsFileProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun getType(uri: Uri): String? = null

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, fileMode: String): ParcelFileDescriptor? {
        context?.cacheDir?.let { cacheDir ->
            uri.path?.let { path ->
                val file = File(cacheDir, path)
                val mode = if (TextUtils.equals(fileMode, "r")) ParcelFileDescriptor.MODE_READ_ONLY else (ParcelFileDescriptor.MODE_WRITE_ONLY
                        or ParcelFileDescriptor.MODE_TRUNCATE
                        or ParcelFileDescriptor.MODE_CREATE)
                return ParcelFileDescriptor.open(file, mode)
            }
        }
        throw FileNotFoundException()
    }

    companion object {
        fun getAuthority(context: Context) = "${context.packageName}.MmsFileProvider"
    }
}