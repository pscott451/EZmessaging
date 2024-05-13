package com.scott.ezmessaging.extension

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.util.Log

internal fun ContentResolver?.getCursor(
    uri: Uri,
    columnsToReturn: Array<String>? = null,
    columnFilters: String? = null
): Cursor? = this?.query(uri, columnsToReturn, columnFilters, null, null, null)

internal fun Cursor.getColumnValue(columnId: String): String? {
    val index = getColumnIndex(columnId)
    return if (index == -1) null else getString(index)
}

internal fun Cursor.printAllColumns() {
    columnNames.forEach { column ->
        val columnIndex = getColumnIndex(column)
        if (columnIndex > -1) {
            val value = getString(columnIndex)
            Log.i("", "column: $column \t value: $value")
        }
    }
    Log.i("", "************************************ \n\n\n\n")
}