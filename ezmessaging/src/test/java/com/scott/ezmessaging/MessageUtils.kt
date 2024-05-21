package com.scott.ezmessaging

import com.scott.ezmessaging.model.Message

object MessageUtils {

    fun buildSmsMessage(
        messageId: String = "messageId",
        threadId: String = "threadId",
        senderAddress: String = "senderAddress",
        dateSent: Long = 1L,
        dateReceived: Long = 2L,
        hasBeenRead: Boolean = false,
        participants: Set<String> = emptySet(),
        text: String = "text"
    ) = Message.SmsMessage(
        messageId,
        threadId,
        senderAddress,
        dateSent,
        dateReceived,
        hasBeenRead,
        participants,
        text
    )

    fun buildMmsMessage(
        messageId: String = "messageId",
        threadId: String = "threadId",
        senderAddress: String = "senderAddress",
        dateSent: Long = 1L,
        dateReceived: Long = 2L,
        hasBeenRead: Boolean = false,
        participants: Set<String> = emptySet(),
        uniqueId: String = "uniqueId",
        messageType: String = "messageType",
        hasImage: Boolean = false,
        text: String? = "text"
    ) = Message.MmsMessage(
        messageId,
        threadId,
        senderAddress,
        dateSent,
        dateReceived,
        hasBeenRead,
        participants,
        uniqueId,
        messageType,
        hasImage,
        text
    )
}