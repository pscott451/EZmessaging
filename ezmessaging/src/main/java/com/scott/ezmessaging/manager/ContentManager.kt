package com.scott.ezmessaging.manager

import android.content.Intent
import com.scott.ezmessaging.model.Initializable
import com.scott.ezmessaging.model.Initializable.Initialized
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.Message.MmsMessage
import com.scott.ezmessaging.model.Message.SmsMessage
import com.scott.ezmessaging.model.MessageData
import com.scott.ezmessaging.model.MessageReceiveResult
import com.scott.ezmessaging.model.MessageSendResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Responsible for managing all sms and mms messages.
 * Sending and receiving operations switch to the IO dispatcher but will always resume on main.
 */
interface ContentManager {

    /**
     * A flow that emits an [Initializable].
     * Once the state is [Initialized], the content manager is ready to use.
     */
    val initializedState: StateFlow<Initializable<Unit>>

    /**
     * Should only be called AFTER all of the messaging permissions have been granted.
     */
    fun initialize()

    /**
     * Retrieves all of the existing [MmsMessage]/[SmsMessage].
     */
    suspend fun getAllMessages(): List<Message>

    /**
     * Marks a [Message] as read.
     * @return true, if the message was successfully updated.
     */
    fun markMessageAsRead(message: Message): Boolean

    /**
     * Deletes a [Message]
     * @return true, if the message was successfully deleted.
     */
    fun deleteMessage(message: Message): Boolean

    /**
     * Handles receiving both SMS and MMS messages.
     * @param intent the intent received from the broadcast.
     * @param onReceiveResult invoked with a [MessageReceiveResult] when completed.
     */
    fun receiveMessage(
        intent: Intent,
        onReceiveResult: (MessageReceiveResult) -> Unit
    )

    /**
     * Sends an sms message.
     * @param address the recipient address.
     * @param text the text.
     * @param onSent invoked with [MessageSendResult.Success] if the message was successfully sent.
     * @param onDelivered invoked with true if the message was successfully delivered to the recipient. Depending
     * on the carrier, this may not always be available.
     */
    fun sendSmsMessage(
        address: String,
        text: String,
        onSent: (MessageSendResult) -> Unit,
        onDelivered: (Boolean) -> Unit
    )

    /**
     * Sends an mms message.
     * @param message The [MessageData] to send.
     * @param recipients a list of recipients.
     * @param onSent invoked with [MessageSendResult.Success] if the message was successfully sent.
     */
    fun sendMmsMessage(
        message: MessageData,
        recipients: Array<String>,
        onSent: (MessageSendResult) -> Unit
    )

    /**
     * @return a list of messages, SMS and MMS, that match the provided params, if they exist.
     * @param text The text content of the message.
     * @param afterDateMillis returns all messages after the date.
     */
    suspend fun getMessagesByParams(
        text: String? = null,
        afterDateMillis: Long? = null
    ): List<Message>

    object SupportedMessageTypes {
        const val CONTENT_TYPE_JPEG = "image/jpeg"
        const val CONTENT_TYPE_JPG = "image/jpg"
        const val CONTENT_TYPE_BMP = "image/bmp"
        const val CONTENT_TYPE_GIF = "image/gif" // Able to receive, but not send currently
        const val CONTENT_TYPE_PNG = "image/png"
        const val CONTENT_TYPE_TEXT = "text/plain"

        fun String?.isValidMessageType() = this == CONTENT_TYPE_JPEG || this == CONTENT_TYPE_JPG ||
                this == CONTENT_TYPE_GIF || this == CONTENT_TYPE_BMP ||
                this == CONTENT_TYPE_PNG || this == CONTENT_TYPE_TEXT
    }
}