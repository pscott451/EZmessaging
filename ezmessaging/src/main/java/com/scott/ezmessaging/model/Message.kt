package com.scott.ezmessaging.model

import android.content.ContentResolver
import com.scott.ezmessaging.model.Message.MmsMessage
import com.scott.ezmessaging.model.Message.SmsMessage

/**
 * A sealed class that represents all messages.
 *
 * @property messageId the ID of the message. This will be unique with every [SmsMessage] object returned from a [ContentResolver].
 * This will not be unique with [MmsMessage]. E.g. The [ContentResolver] may return 2 entries for a gif with the same [messageId] - one with a content type of 'application/smil' and
 * one with a content type of 'image/gif'.
 * @property threadId the thread this message belongs to.
 * @property senderAddress the contact that sent the message.
 * @property dateSent the date in millis when the message was sent. If sent from this device, this will be the same as the [dateReceived].
 * @property dateReceived the date in millis when the message was received.
 * @property hasBeenRead true, if the user has read the message.
 * @property participants a list of everyone involved in the conversation, including the user.
 */
sealed class Message {
    abstract val messageId: String
    abstract val threadId: String
    abstract val senderAddress: String
    abstract val dateSent: Long
    abstract val dateReceived: Long
    abstract val hasBeenRead: Boolean
    abstract val participants: Set<String>

    /**
     * Just a plain old text message.
     * @param text the content of the message.
     */
    data class SmsMessage(
        override val messageId: String,
        override val threadId: String,
        override val senderAddress: String,
        override val dateSent: Long,
        override val dateReceived: Long,
        override val hasBeenRead: Boolean,
        override val participants: Set<String>,
        val text: String
    ): Message()

    /**
     * Pictures, Gifs, group conversations, etc.
     * @property uniqueId every object returned from a [ContentResolver] will have a unique id.
     * @property messageType the content type of the message. 'image/gif', 'image/jpg', 'text/plain', etc.
     * @property hasImage if there's an image attached to the message.
     * @property text the content of the message.
     */
    data class MmsMessage(
        override val messageId: String,
        override val threadId: String,
        override val senderAddress: String,
        override val dateSent: Long,
        override val dateReceived: Long,
        override val hasBeenRead: Boolean,
        override val participants: Set<String>,
        val uniqueId: String,
        val messageType: String,
        val hasImage: Boolean,
        val text: String?
    ): Message()
}