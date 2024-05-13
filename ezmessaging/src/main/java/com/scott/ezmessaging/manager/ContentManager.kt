package com.scott.ezmessaging.manager

import android.content.Intent
import com.scott.ezmessaging.model.Initializable
import com.scott.ezmessaging.model.Initializable.Initialized
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.Message.MmsMessage
import com.scott.ezmessaging.model.Message.SmsMessage
import com.scott.ezmessaging.model.MessageData
import kotlinx.coroutines.flow.StateFlow

/**
 * Responsible for managing all sms and mms messages.
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
     * @return the list of [Message] that was successfully received.
     * The list may contain null values if there was an error receiving the messages.
     * If the list was empty, it indicates the received message was neither sms or mms.
     */
    suspend fun receiveMessage(intent: Intent): List<Message?>

    /**
     * Sends an sms message.
     * @return the [SmsMessage] if it was successfully sent and inserted into the database. null, otherwise.
     */
    fun sendSmsMessage(address: String, text: String, threadId: String): SmsMessage?

    /**
     * Sends an mms message.
     * @return the [MmsMessage] if it was successfully sent and inserted into the database. null, otherwise.
     */
    suspend fun sendMmsMessage(
        message: MessageData,
        recipients: Array<String>
    ): MmsMessage?

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