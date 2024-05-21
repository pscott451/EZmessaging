package com.scott.ezmessaging.contentresolver

import android.content.ContentResolver
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle

class TestCursor(
    content: List<TestCursorObject>,
    columnsToReturn: Array<String>? = null
) : Cursor {

    private val filteredContent = buildFilteredContent(content, columnsToReturn)

    private var cursorPosition = -1

    override fun moveToNext(): Boolean {
        cursorPosition++
        return cursorPosition <= filteredContent.lastIndex
    }

    override fun getColumnNames(): Array<String> = getAllColumnNames()

    override fun getColumnIndex(columnName: String?): Int {
        val contentInScope = filteredContent[cursorPosition]
        contentInScope.columnEntries.forEachIndexed { i, columnEntry ->
            if (columnEntry.columnName == columnName) return i
        }
        return -1
    }

    override fun getString(columnIndex: Int): String? = filteredContent[cursorPosition].columnEntries[columnIndex].value

    private fun buildFilteredContent(allContent: List<TestCursorObject>, columnsToReturn: Array<String>?): List<TestCursorObject> {
        if (columnsToReturn == null) return allContent
        val filteredObjects = arrayListOf<TestCursorObject>()
        val filteredEntries = arrayListOf<TestCursorColumnEntry>()
        allContent.forEach { testCursorObject ->
            testCursorObject.columnEntries.forEach { columnEntry ->
                if (columnsToReturn.contains(columnEntry.columnName)) filteredEntries.add(columnEntry)
            }
            filteredObjects.add(TestCursorObject(filteredEntries.map { it }))
            filteredEntries.clear()
        }
        return filteredObjects
    }

    private fun getAllColumnNames(): Array<String> {
        val columns = mutableSetOf<String>()
        filteredContent.forEach { cursorContent ->
            columns.addAll(cursorContent.columnEntries.map { it.columnName })
        }
        return columns.toTypedArray()
    }


    /* Shouldn't need the rest of these for testing */

    override fun close() {}

    override fun getCount(): Int = 0

    override fun getPosition(): Int = 0

    override fun move(p0: Int): Boolean = false

    override fun moveToPosition(p0: Int): Boolean = false

    override fun moveToFirst(): Boolean = false

    override fun moveToLast(): Boolean = false

    override fun moveToPrevious(): Boolean = false

    override fun isFirst(): Boolean = false

    override fun isLast(): Boolean = false

    override fun isBeforeFirst(): Boolean = false

    override fun isAfterLast(): Boolean = false

    override fun getColumnIndexOrThrow(p0: String?): Int = 0

    override fun getColumnName(p0: Int): String = ""

    override fun getColumnCount(): Int = 0

    override fun getBlob(p0: Int): ByteArray = byteArrayOf()

    override fun copyStringToBuffer(p0: Int, p1: CharArrayBuffer?) {}

    override fun getShort(p0: Int): Short = 0

    override fun getInt(p0: Int): Int = 0

    override fun getLong(p0: Int): Long = 0L

    override fun getFloat(p0: Int): Float = 0f

    override fun getDouble(p0: Int): Double = 0.0

    override fun getType(p0: Int): Int = 0

    override fun isNull(p0: Int): Boolean = false

    override fun deactivate() {}

    override fun requery(): Boolean = false

    override fun isClosed(): Boolean = false

    override fun registerContentObserver(p0: ContentObserver?) {}

    override fun unregisterContentObserver(p0: ContentObserver?) {}

    override fun registerDataSetObserver(p0: DataSetObserver?) {}

    override fun unregisterDataSetObserver(p0: DataSetObserver?) {}

    override fun setNotificationUri(p0: ContentResolver?, p1: Uri?) {}

    override fun getNotificationUri(): Uri = Uri.EMPTY

    override fun getWantsAllOnMoveCalls(): Boolean = false

    override fun setExtras(p0: Bundle?) {}

    override fun getExtras(): Bundle = Bundle()

    override fun respond(p0: Bundle?): Bundle = Bundle()
}

data class TestCursorObject(
    val columnEntries: List<TestCursorColumnEntry>
)

data class TestCursorColumnEntry(
    val columnName: String,
    val value: String?
)