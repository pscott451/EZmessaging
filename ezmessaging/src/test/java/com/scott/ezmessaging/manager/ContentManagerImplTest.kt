package com.scott.ezmessaging.manager

import android.content.Intent
import android.provider.Telephony
import app.cash.turbine.test
import com.google.android.mms.ContentType
import com.scott.ezmessaging.MainCoroutineRule
import com.scott.ezmessaging.MessageUtils
import com.scott.ezmessaging.UnconfinedCoroutineRule
import com.scott.ezmessaging.model.Initializable
import com.scott.ezmessaging.model.MessageData
import com.scott.ezmessaging.model.MessageReceiveResult
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(UnconfinedCoroutineRule::class)
class ContentManagerImplTest {

    private val smsManager = mockk<SmsManager>(relaxed = true)
    private val mmsManager = mockk<MmsManager>(relaxed = true)
    private val deviceManager = mockk<DeviceManager>(relaxed = true)

    private val contentManager = ContentManagerImpl(
        smsManager = smsManager,
        mmsManager = mmsManager,
        deviceManager = deviceManager,
        dispatcherProvider = MainCoroutineRule.dispatcherProvider
    )

    @BeforeEach
    fun setup() {
        mockkStatic(Telephony.Sms.Intents::class)
    }

    @Test
    fun `initializedState is Uninitialized when the deviceManager is Uninitialized`() = runTest {
        // Given
        every { deviceManager.initializedState } returns MutableStateFlow(Initializable.Uninitialized)

        // When
        contentManager.initialize()

        // Then
        contentManager.initializedState.test {
            awaitItem().shouldBe(Initializable.Uninitialized)
        }
    }

    @Test
    fun `initializedState is Error when the deviceManager is Error`() = runTest {
        // Given
        val error = Initializable.Error(Throwable())
        every { deviceManager.initializedState } returns MutableStateFlow(error)

        // When
        contentManager.initialize()

        // Then
        contentManager.initializedState.test {
            awaitItem().shouldBe(error)
        }
    }

    @Test
    fun `initializedState is Initialized when the deviceManager is Initialized`() = runTest {
        // Given
        every { deviceManager.initializedState } returns MutableStateFlow(Initializable.Initialized(Unit))

        // When
        contentManager.initialize()

        // Then
        contentManager.initializedState.test {
            awaitItem().shouldBe(Initializable.Initialized(Unit))
        }
    }

    @Test
    fun `getAllMessages returns sms and mms messages`() = runTest {
        // Given
        val smsMessage = MessageUtils.buildSmsMessage()
        val mmsMessage = MessageUtils.buildMmsMessage()
        coEvery { smsManager.getAllMessages() } returns listOf(smsMessage)
        coEvery { mmsManager.getAllMessages() } returns listOf(mmsMessage)

        // When
        val messages = contentManager.getAllMessages()

        // Then
        messages.shouldBe(listOf(smsMessage, mmsMessage))
    }

    @Test
    fun `intent message gets received as sms if it's an sms message`() = runTest {
        // Given
        val intent = mockk<Intent>(relaxed = true).apply {
            every { action } returns Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        }
        val smsMessage = mockk<android.telephony.SmsMessage>(relaxed = true).apply {
            every { originatingAddress } returns "5555555555"
            every { messageBody } returns "body"
            every { timestampMillis } returns 1L
        }
        every { Telephony.Sms.Intents.getMessagesFromIntent(intent) } returns arrayOf(smsMessage)

        // When
        contentManager.receiveMessage(intent, {})

        // Then
        verify { smsManager.receiveMessage("5555555555", "body", 1, any()) }
    }

    @Test
    fun `intent message gets received as sms and onReceiveResult is invoked with Success if the list is not empty`() = runTest {
        // Given
        val onReceiveResult = mockk<(MessageReceiveResult) -> Unit>(relaxed = true)
        val intent = mockk<Intent>(relaxed = true).apply {
            every { action } returns Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        }
        val smsMessage = mockk<android.telephony.SmsMessage>(relaxed = true).apply {
            every { originatingAddress } returns "5555555555"
            every { messageBody } returns "body"
            every { timestampMillis } returns 1L
        }
        every { Telephony.Sms.Intents.getMessagesFromIntent(intent) } returns arrayOf(smsMessage)
        every { smsManager.receiveMessage(any(), any(), any(), any()) } returns MessageUtils.buildSmsMessage()

        // When
        contentManager.receiveMessage(intent, onReceiveResult)

        // Then
        verify { smsManager.receiveMessage("5555555555", "body", 1, any()) }
        verify { onReceiveResult(MessageReceiveResult.Success(listOf(MessageUtils.buildSmsMessage()))) }
    }

    @Test
    fun `intent message gets received as sms and onReceiveResult is invoked with Failed if the list is empty`() = runTest {
        // Given
        val onReceiveResult = mockk<(MessageReceiveResult) -> Unit>(relaxed = true)
        val intent = mockk<Intent>(relaxed = true).apply {
            every { action } returns Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        }
        every { Telephony.Sms.Intents.getMessagesFromIntent(intent) } returns arrayOf()

        // When
        contentManager.receiveMessage(intent, onReceiveResult)

        // Then
        verify { onReceiveResult(MessageReceiveResult.Failed("Failed to insert the messages into the database")) }
    }

    @Test
    fun `intent message gets received as mms if the intent message is mms`() = runTest {
        // Given
        val intent = mockk<Intent>(relaxed = true).apply {
            every { action } returns Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION
            every { type } returns ContentType.MMS_MESSAGE
        }

        // When
        contentManager.receiveMessage(intent) {}

        // Then
        verify { mmsManager.receiveMessage(intent, any()) }
    }

    @Test
    fun `onReceiveResult gets invoked with Failed if the intent is neither sms or mms`() = runTest {
        // Given
        val onReceiveResult = mockk<(MessageReceiveResult) -> Unit>(relaxed = true)
        val intent = mockk<Intent>(relaxed = true).apply {
            every { action } returns "Garbage"
        }

        // When
        contentManager.receiveMessage(intent, onReceiveResult)

        // Then
        verify { onReceiveResult(MessageReceiveResult.Failed("The received intent was neither sms or mms")) }
    }

    @Test
    fun `sendSmsMessage calls smsManager sendMessage`() = runTest {
        // Given
        val address = "5555555555"
        val text = "text"

        // When
        contentManager.sendSmsMessage(address, text, {}, {})

        // Then
        verify { smsManager.sendMessage(address, text, any(), any()) }
    }

    @Test
    fun `sendMmsMessage calls mmsManager sendMessage`() = runTest {
        // Given
        val messageData = MessageData.Text("text")
        val recipients = arrayOf("5555555555")

        // When
        contentManager.sendMmsMessage(messageData, recipients, {})

        // Then
        verify { mmsManager.sendMessage(messageData, recipients, any()) }
    }

    @Test
    fun `markMessageAsRead marks sms message as read if it's an SmsMessage`() = runTest {
        // Given
        val message = MessageUtils.buildSmsMessage()

        // When
        contentManager.markMessageAsRead(message)

        // Then
        verify { smsManager.markMessageAsRead(message.messageId) }
    }

    @Test
    fun `markMessageAsRead marks mms message as read if it's an MmsMessage`() = runTest {
        // Given
        val message = MessageUtils.buildMmsMessage()

        // When
        contentManager.markMessageAsRead(message)

        // Then
        verify { mmsManager.markMessageAsRead(message.messageId) }
    }

    @Test
    fun `deleteMessage deletes sms message if it's an SmsMessage`() = runTest {
        // Given
        val message = MessageUtils.buildSmsMessage()

        // When
        contentManager.deleteMessage(message)

        // Then
        verify { smsManager.deleteMessage(message.messageId) }
    }

    @Test
    fun `deleteMessage deletes mms message if it's an MmsMessage`() = runTest {
        // Given
        val message = MessageUtils.buildMmsMessage()

        // When
        contentManager.deleteMessage(message)

        // Then
        verify { mmsManager.deleteMessage(message.messageId) }
    }

    @Test
    fun `getMessagesByParams returns found messages from smsManager and mmsManager`() = runTest {
        // Given
        val smsMessages = MessageUtils.buildSmsMessage()
        val mmsMessages = MessageUtils.buildMmsMessage()
        every { smsManager.findMessages("text", 1L) } returns listOf(smsMessages)
        every { mmsManager.findMessages("text", 1L) } returns listOf(mmsMessages)

        // When
        val messages = contentManager.getMessagesByParams("text", 1L)

        // Then
        messages.shouldBe(listOf(mmsMessages, smsMessages))
    }

}