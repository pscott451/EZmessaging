package com.scott.ezmessaging.manager

import android.content.Intent
import com.scott.ezmessaging.contentresolver.MmsContentResolver
import com.scott.ezmessaging.model.GoogleProcessResult
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.Message.MmsMessage
import com.scott.ezmessaging.model.MessageData
import com.scott.ezmessaging.model.MessageReceiveResult
import com.scott.ezmessaging.model.MessageSendResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responsible for handling all [MmsMessage].
 */
@Singleton
internal class MmsManager @Inject constructor(
    private val mmsContentResolver: MmsContentResolver,
    private val googleManager: GoogleManager,
    private val deviceManager: DeviceManager
) {

    /**
     * Retrieve all messages from the database, if they exist.
     */
    suspend fun getAllMessages(): List<MmsMessage> = mmsContentResolver.getAllMmsMessages()

    /**
     * @return a list of [MmsMessage] that match the provided params, if they exist.
     * @param exactText returns any messages that match the provided text exactly.
     * @param containsText returns any messages that contain the provided text.
     * @param afterDateMillis returns all messages after the date.
     */
    fun findMessages(
        exactText: String? = null,
        containsText: String? = null,
        afterDateMillis: Long? = null
    ): List<Message> = mmsContentResolver.findMessages(
        exactText = exactText,
        containsText = containsText,
        afterDateMillis = afterDateMillis
    )

    /**
     * Handles receiving a message from another device.
     */
    fun receiveMessage(
        intent: Intent,
        onReceiveResult: (MessageReceiveResult) -> Unit
    ) {
        googleManager.parseReceivedMmsIntent(intent) { processResult ->
            val receiveResult = when (processResult) {
                is GoogleProcessResult.ProcessFailed -> {
                    MessageReceiveResult.Failed(processResult.errorMessage)
                }

                is GoogleProcessResult.ProcessSuccess -> {
                    mmsContentResolver.findMessageByUri(processResult.saveLocation)?.let {
                        MessageReceiveResult.Success(listOf(it))
                    } ?: run {
                        MessageReceiveResult.Failed("No messages found after parsing intent")
                    }
                }
            }
            onReceiveResult(receiveResult)
        }
    }

    /**
     * Sends an MMS message to another device and inserts the value into the content resolver.
     */
    fun sendMessage(
        message: MessageData,
        recipients: Array<String>,
        onMessageCreated: (MmsMessage?) -> Unit,
        onSent: (MessageSendResult) -> Unit
    ) {
        googleManager.sendMmsMessage(
            message = message,
            fromAddress = deviceManager.getThisDeviceMainNumber(),
            recipients = recipients,
            onInsertedIntoDatabase = { location ->
                onMessageCreated(mmsContentResolver.findMessageByUri(location))
            },
            onSent = { uri, exception ->
                mmsContentResolver.findMessageByUri(uri)?.let { message ->
                    onSent(MessageSendResult.Success(message))
                } ?: run {
                    val errorMessage = exception?.message ?: "An error occurred sending the mms message"
                    onSent(MessageSendResult.Failed(errorMessage))
                }
            }
        )
    }

    /**
     * Marks a message with the provided [messageId] as read.
     * @return true, if the message was successfully updated.
     */
    fun markMessageAsRead(messageId: String) = mmsContentResolver.markMessageAsRead(messageId)

    /**
     * Deletes a message with the provided [messageId].
     * @return true, if the message was successfully deleted.
     */
    fun deleteMessage(messageId: String) = mmsContentResolver.deleteMessage(messageId)

}
