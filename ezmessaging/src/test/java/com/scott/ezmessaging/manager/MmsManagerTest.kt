package com.scott.ezmessaging.manager

import android.content.Intent
import com.scott.ezmessaging.MessageUtils
import com.scott.ezmessaging.UnconfinedCoroutineRule
import com.scott.ezmessaging.contentresolver.MmsContentResolver
import com.scott.ezmessaging.model.MessageData
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(UnconfinedCoroutineRule::class)
class MmsManagerTest {

    private val mmsContentResolver = mockk<MmsContentResolver>(relaxed = true)
    private val googleManager = mockk<GoogleManager>(relaxed = true)
    private val deviceManager = mockk<DeviceManager>(relaxed = true)

    private val mmsManager = MmsManager(
        mmsContentResolver,
        googleManager,
        deviceManager
    )

    @Test
    fun `getAllMessages returns messages from mmsContentResolver`() = runTest {
        // Given
        coEvery { mmsContentResolver.getAllMmsMessages() } returns listOf(MessageUtils.buildMmsMessage())

        // When
        val messages = mmsManager.getAllMessages()

        // Then
        messages.shouldBe(listOf(MessageUtils.buildMmsMessage()))
    }

    @Test
    fun `findMessages returns messages from mmsContentResolver`() = runTest {
        // Given
        val text = "text"
        val afterDate = 1L
        coEvery { mmsContentResolver.findMessages(exactText = text, afterDateMillis = afterDate) } returns listOf(MessageUtils.buildMmsMessage())

        // When
        val messages = mmsManager.findMessages(exactText = text, afterDateMillis = afterDate)

        // Then
        messages.shouldBe(listOf(MessageUtils.buildMmsMessage()))
    }

    @Test
    fun `googleManager parses intent when receiveMessage is called`() = runTest {
        // Given
        val receivedIntent = Intent()

        // When
        mmsManager.receiveMessage(receivedIntent, {})

        // Then
        verify { googleManager.parseReceivedMmsIntent(receivedIntent, any()) }
    }

    @Test
    fun `googleManager sends message when sendMessage is called`() = runTest {
        // Given
        val messageData = MessageData.Text("text")
        val recipients = arrayOf("1111111111")
        every { deviceManager.getThisDeviceMainNumber() } returns "5555555555"

        // When
        mmsManager.sendMessage(messageData, recipients, {})

        // Then
        verify { googleManager.sendMmsMessage(messageData, "5555555555", recipients, any()) }
    }

    @Test
    fun `mmsContentResolver marks message as read when markMessageAsRead is called`() = runTest {
        // Given
        val messageId = "id"

        // When
        mmsManager.markMessageAsRead(messageId)

        // Then
        verify { mmsContentResolver.markMessageAsRead(messageId) }
    }

    @Test
    fun `mmsContentResolver deletes message when deleteMessage is called`() = runTest {
        // Given
        val messageId = "id"

        // When
        mmsManager.deleteMessage(messageId)

        // Then
        verify { mmsContentResolver.deleteMessage(messageId) }
    }
}