package com.scott.ezmessaging.manager

import android.content.Context
import android.telephony.SmsManager
import com.scott.ezmessaging.contentresolver.SmsContentResolver
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.Message.SmsMessage
import com.scott.ezmessaging.model.MessageSendResult
import com.scott.ezmessaging.provider.DispatcherProvider
import com.scott.ezmessaging.receiver.SmsDeliveredBroadcastReceiver
import com.scott.ezmessaging.receiver.SmsSentBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Responsible for handling all [SmsMessage].
 */
@Singleton
internal class SmsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsContentResolver: SmsContentResolver,
    dispatcherProvider: DispatcherProvider,
) {

    private val systemSmsManager: SmsManager? = context.getSystemService(SmsManager::class.java)

    private val coroutineScope = CoroutineScope(dispatcherProvider.io())

    /**
     * Retrieve the messages from the database.
     */
    suspend fun getAllMessages(): List<SmsMessage> = suspendCoroutine { continuation ->
        coroutineScope.launch {
            val sentMessagesDeferred = async { smsContentResolver.getAllSentSmsMessages() }
            val receivedMessagesDeferred = async { smsContentResolver.getAllReceivedSmsMessages() }
            val messages = sentMessagesDeferred.await() + receivedMessagesDeferred.await()
            continuation.resume(messages)
        }
    }

    /**
     * @return a list of [SmsMessage] that match the provided params, if they exist.
     * @param text The text content of the message.
     * @param afterDateMillis returns all messages after the date.
     */
    fun findMessages(
        text: String? = null,
        afterDateMillis: Long? = null
    ): List<Message> = smsContentResolver.findMessages(text = text, afterDateMillis = afterDateMillis)

    /**
     * Handles receiving a message.
     * @return the [SmsMessage] if it was inserted. null, if it fails.
     */
    fun receiveMessage(address: String, body: String, dateSent: Long, dateReceived: Long): SmsMessage? {
        return smsContentResolver.insertReceivedMessage(
            address = address,
            body = body,
            dateSent = dateSent,
            dateReceived = dateReceived
        )
    }

    /**
     * Sends an SMS message to another device and inserts the value into the content resolver.
     */
    fun sendMessage(
        address: String,
        text: String,
        onSent: (MessageSendResult) -> Unit,
        onDelivered: (Boolean) -> Unit
    ) {
        if (address.isEmpty() || text.isEmpty()) {
            onSent.invoke(MessageSendResult.Failed("Address and text cannot be empty"))
        } else {
            val insertedMessage = smsContentResolver.insertSentMessage(address, text)
            val sentIntent = SmsSentBroadcastReceiver().buildPendingIntent(context, onSent)
            val deliveredIntent = SmsDeliveredBroadcastReceiver().buildPendingIntent(context) { isSuccess ->
                onDelivered(isSuccess)
                if (isSuccess) markMessageAsDelivered(insertedMessage?.messageId)
            }
            systemSmsManager?.sendTextMessage(address, null, text, sentIntent, deliveredIntent) ?: run {
                onSent.invoke(MessageSendResult.Failed("No SmsManager detected. Are you using an Emulator?"))
            }
        }
    }

    /**
     * Marks a message with the provided [messageId] as read.
     * @return true, if the message was successfully marked as read.
     */
    fun markMessageAsRead(messageId: String): Boolean {
        return smsContentResolver.markMessageAsRead(messageId)
    }

    /**
     * Deletes a message with the provided [messageId].
     * @return true, if the message was successfully deleted.
     */
    fun deleteMessage(messageId: String): Boolean {
        return smsContentResolver.deleteMessage(messageId)
    }

    /**
     * Marks a message as successfully delivered.
     */
    private fun markMessageAsDelivered(messageId: String?) {
        messageId?.let {
            smsContentResolver.markMessageAsDelivered(messageId)
        }
    }
}