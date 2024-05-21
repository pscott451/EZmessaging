package com.scott.ezmessaging.manager

import android.content.Context
import com.scott.ezmessaging.MainCoroutineRule
import com.scott.ezmessaging.MessageUtils
import com.scott.ezmessaging.UnconfinedCoroutineRule
import com.scott.ezmessaging.contentresolver.SmsContentResolver
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(UnconfinedCoroutineRule::class)
class SmsManagerTest {

    private val smsContentResolver = mockk<SmsContentResolver>(relaxed = true)
    private val systemSmsManager = mockk<android.telephony.SmsManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true).apply {
        every { getSystemService(android.telephony.SmsManager::class.java) } returns systemSmsManager
    }

    private val smsManager = SmsManager(
        context,
        smsContentResolver,
        MainCoroutineRule.dispatcherProvider
    )

    @Test
    fun `getAllMessages returns messages from smsContentResolver`() = runTest {
        // Given
        val sentMessage = MessageUtils.buildSmsMessage("sent")
        val receivedMessage = MessageUtils.buildSmsMessage("received")
        coEvery { smsContentResolver.getAllSentSmsMessages() } returns listOf(sentMessage)
        coEvery { smsContentResolver.getAllReceivedSmsMessages() } returns listOf(receivedMessage)

        // When
        val messages = smsManager.getAllMessages()

        // Then
        messages.shouldBe(listOf(sentMessage, receivedMessage))
    }

    @Test
    fun `findMessages returns messages from smsContentResolver`() = runTest {
        // Given
        val text = "text"
        val afterDate = 1L
        coEvery { smsContentResolver.findMessages(text = text, afterDateMillis = afterDate) } returns listOf(MessageUtils.buildSmsMessage())

        // When
        val messages = smsManager.findMessages(text = text, afterDateMillis = afterDate)

        // Then
        messages.shouldBe(listOf(MessageUtils.buildSmsMessage()))
    }

    @Test
    fun `receiveMessage inserts the message into the smsContentResolver`() = runTest {
        // Given
        val address = "1111111111"
        val body = "body"
        val dateSent = 1L
        val dateReceived = 2L

        // When
        smsManager.receiveMessage(address, body, dateSent, dateReceived)

        // Then
        verify { smsContentResolver.insertReceivedMessage(address, body, dateSent, dateReceived) }
    }

    @Test
    fun `smsContentResolver marks message as read when markMessageAsRead is called`() = runTest {
        // Given
        val messageId = "id"

        // When
        smsManager.markMessageAsRead(messageId)

        // Then
        verify { smsContentResolver.markMessageAsRead(messageId) }
    }

    @Test
    fun `smsContentResolver deletes message when deleteMessage is called`() = runTest {
        // Given
        val messageId = "id"

        // When
        smsManager.deleteMessage(messageId)

        // Then
        verify { smsContentResolver.deleteMessage(messageId) }
    }
}