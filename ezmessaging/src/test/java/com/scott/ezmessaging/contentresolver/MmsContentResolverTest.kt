package com.scott.ezmessaging.contentresolver

import android.content.ContentResolver
import android.content.Context
import com.scott.ezmessaging.MainCoroutineRule
import com.scott.ezmessaging.UnconfinedCoroutineRule
import com.scott.ezmessaging.extension.getCursor
import com.scott.ezmessaging.manager.DeviceManager
import com.scott.ezmessaging.model.Message
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(UnconfinedCoroutineRule::class)
class MmsContentResolverTest {

    private val contentResolver = mockk<ContentResolver>(relaxed = true)
    private val context = mockk<Context>(relaxed = true).also {
        every { it.contentResolver } returns contentResolver
    }
    private val deviceManager = mockk<DeviceManager>().also {
        every { it.getThisDeviceMainNumber() } returns "5555555555"
    }

    private val mmsContentResolver = MmsContentResolver(context, MainCoroutineRule.dispatcherProvider, deviceManager)

    @BeforeEach
    fun setup() {
        mockkStatic("com.scott.ezmessaging.extension.CursorExtensionsKt")
    }

    @Test
    fun `getAllMmsMessages returns nothing if required information for each message doesn't exist`() = runTest {
        // Given
        mockCursor("content://mms", METADATA_COLUMNS, TestContentResolverDataMMS.METADATA_RANDOM)
        mockCursor("content://mms/addr", ADDRESSES_COLUMNS, TestContentResolverDataMMS.ADDRESSES_RANDOM)
        mockCursor("content://mms/part", CONTENT_COLUMNS, TestContentResolverDataMMS.CONTENT_RANDOM)

        // When
        val messages = mmsContentResolver.getAllMmsMessages()

        // Then
        messages.shouldBeEmpty()
    }

    @Test
    fun `getAllMmsMessages returns expected messages if required information for each message exists`() = runTest {
        // Given
        val expected = listOf(
            Message.MmsMessage(
                messageId = "43533",
                threadId = "12",
                senderAddress = "1111111111",
                dateSent = 1710452812000,
                dateReceived = 1710452812000,
                hasBeenRead = true,
                participants = setOf("1111111111"),
                uniqueId = "59009",
                messageType = "image/jpeg",
                hasImage = true,
                text = null
            )
        )
        mockCursor("content://mms", METADATA_COLUMNS, TestContentResolverDataMMS.METADATA_43533)
        mockCursor("content://mms/addr", ADDRESSES_COLUMNS, TestContentResolverDataMMS.ADDRESSES_43533)
        mockCursor("content://mms/part", CONTENT_COLUMNS, TestContentResolverDataMMS.CONTENT_43533)

        // When
        val messages = mmsContentResolver.getAllMmsMessages()

        // Then
        messages.shouldBe(expected)
    }

    @Test
    fun `getAllMmsMessages uses correct content object when there are multiple`() = runTest {
        // Given
        val expected = listOf(
            Message.MmsMessage(
                messageId = "44052",
                threadId = "4",
                senderAddress = "5555555555",
                dateSent = 1711319465000,
                dateReceived = 1711319465000,
                hasBeenRead = true,
                participants = setOf("1111111111", "2222222222", "3333333333"),
                uniqueId = "59606",
                messageType = "text/plain",
                hasImage = false,
                text = "Damn, Gina. That's shitty"
            )
        )
        mockCursor("content://mms", METADATA_COLUMNS, TestContentResolverDataMMS.METADATA_44052)
        mockCursor("content://mms/addr", ADDRESSES_COLUMNS, TestContentResolverDataMMS.ADDRESSES_44052)
        mockCursor("content://mms/part", CONTENT_COLUMNS, TestContentResolverDataMMS.CONTENT_44052)

        // When
        val messages = mmsContentResolver.getAllMmsMessages()

        // Then
        messages.shouldBe(expected)
    }

    private fun mockCursor(
        contentUri: String,
        columnsToReturn: Array<String>,
        cursorData: List<TestCursorObject>
    ) {
        every { contentResolver.getCursor(contentUri, columnsToReturn, null) } returns TestCursor(cursorData, columnsToReturn)
    }

    companion object {
        private val METADATA_COLUMNS = arrayOf(
            "_id",
            "thread_id",
            "date_sent",
            "date",
            "read"
        )

        private val ADDRESSES_COLUMNS = arrayOf(
            "address",
            "type",
            "msg_id"
        )

        private val CONTENT_COLUMNS = arrayOf(
            "text",
            "_data",
            "ct",
            "mid",
            "_id"
        )
    }
}