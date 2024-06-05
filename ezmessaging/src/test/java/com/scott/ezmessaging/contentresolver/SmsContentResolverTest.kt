package com.scott.ezmessaging.contentresolver

import android.content.ContentResolver
import android.content.Context
import com.scott.ezmessaging.extension.getCursor
import com.scott.ezmessaging.manager.DeviceManager
import com.scott.ezmessaging.model.Message
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SmsContentResolverTest {

    private val contentResolver = mockk<ContentResolver>(relaxed = true)
    private val context = mockk<Context>(relaxed = true).also {
        every { it.contentResolver } returns contentResolver
    }
    private val deviceManager = mockk<DeviceManager>().also {
        every { it.getThisDeviceMainNumber() } returns "5555555555"
    }

    private val smsContentResolver = SmsContentResolver(context, deviceManager)

    @BeforeEach
    fun setup() {
        mockkStatic("com.scott.ezmessaging.extension.CursorExtensionsKt")
    }

    @Test
    fun `getAllReceivedSmsMessages returns sms messages with correct data`() {
        // Given
        mockCursor(contentUri = "content://sms/inbox", cursorData = TestContentResolverDataSMS.RECEIVED)
        val expected = arrayListOf(
            Message.SmsMessage(
                messageId = "55264",
                threadId = "482",
                senderAddress = "49878",
                dateSent = 1711491254000,
                dateReceived = 1711491255707,
                hasBeenRead = true,
                participants = setOf("49878"),
                text = "All or part of your Samsung order #SA131757595 was delivered. Thank you for choosing Samsung."
            ),
            Message.SmsMessage(
                messageId = "55263",
                threadId = "703",
                senderAddress = "8336430251",
                dateSent = 1711480911000,
                dateReceived = 1711480912446,
                hasBeenRead = true,
                participants = setOf("8336430251"),
                text = "A package is ready for pickup at Package Concierge."
            )
        )

        // When
        val messages = smsContentResolver.getAllReceivedSmsMessages()

        // Then
        messages.shouldBe(expected)
    }

    @Test
    fun `getAllSentSmsMessages returns sms messages with correct data`() {
        // Given
        mockCursor(contentUri = "content://sms/sent", cursorData = TestContentResolverDataSMS.SENT)
        val expected = arrayListOf(
            Message.SmsMessage( // An SMS message to myself
                messageId = "55255",
                threadId = "38",
                senderAddress = "5555555555",
                dateSent = 1710892306823,
                dateReceived = 1710892306823,
                hasBeenRead = true,
                participants = setOf("5555555555"),
                text = "hiiii"
            ),
            Message.SmsMessage(
                messageId = "55218",
                threadId = "11",
                senderAddress = "5555555555",
                dateSent = 1710442650402,
                dateReceived = 1710442650402,
                hasBeenRead = true,
                participants = setOf("2222222222"),
                text = "Hope you bought me a pie"
            )
        )

        // When
        val messages = smsContentResolver.getAllSentSmsMessages()

        // Then
        messages.shouldBe(expected)
    }

    private fun mockCursor(
        contentUri: String,
        cursorData: List<TestCursorObject>,
    ) {
        val columnsToReturn = arrayOf(
            "_id",
            "thread_id",
            "address",
            "date_sent",
            "date",
            "read",
            "body",
        )
        every { contentResolver.getCursor(contentUri, columnsToReturn, any()) } returns TestCursor(cursorData, columnsToReturn)
    }
}