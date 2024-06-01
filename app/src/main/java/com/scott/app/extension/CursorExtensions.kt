package com.scott.app.extension

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

internal fun ContentResolver?.getCursor(
    uri: Uri,
    columnsToReturn: Array<String>? = null,
    columnFilters: String? = null
): Cursor? = this?.query(uri, columnsToReturn, columnFilters, null, null, null)

internal fun Cursor.getColumnValue(columnId: String): String? {
    val index = getColumnIndex(columnId)
    return if (index == -1) null else getString(index)
}