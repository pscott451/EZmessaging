package com.scott.ezmessaging.extension

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CursorExtensionTest {

    private val contentResolver = mockk<ContentResolver>(relaxed = true)
    private val cursor = mockk<Cursor>(relaxed = true)

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
    }

    @Test
    fun `getCursor with uri uses passed arguments`() {
        // Given
        every { Uri.parse("content://sms") } returns mockk()
        val uri = Uri.parse("content://sms")
        val columnsToReturn = arrayOf("column1", "column2")
        val columnFilters = "filters"

        // When
        val cursor = contentResolver.getCursor(uri, columnsToReturn, columnFilters)

        // Then
        verify { contentResolver.query(uri, columnsToReturn, columnFilters, null, null, null) }
    }

    @Test
    fun `getCursor with string uses passed arguments`() {
        // Given
        val mockUri = mockk<Uri>()
        every { Uri.parse("content://sms") } returns mockUri
        val uri = "content://sms"
        val columnsToReturn = arrayOf("column1", "column2")
        val columnFilters = "filters"

        // When
        contentResolver.getCursor(uri, columnsToReturn, columnFilters)

        // Then
        verify { contentResolver.query(mockUri, columnsToReturn, columnFilters, null, null, null) }
    }

    @Test
    fun `getColumnValue returns null if it doesn't exist`() {
        // Given
        every { cursor.getColumnIndex("id") } returns -1

        // When
        val value = cursor.getColumnValue("id")

        // Then
        value.shouldBeNull()
    }

    @Test
    fun `getColumnValue returns value from cursor if it exists`() {
        // Given
        every { cursor.getColumnIndex("id") } returns 1
        every { cursor.getString(1) } returns "value"

        // When
        val value = cursor.getColumnValue("id")

        // Then
        value.shouldBe("value")
    }
}