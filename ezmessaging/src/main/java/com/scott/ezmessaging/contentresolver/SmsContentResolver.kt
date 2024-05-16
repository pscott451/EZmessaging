package com.scott.ezmessaging.contentresolver

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import com.scott.ezmessaging.BuildConfig
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.AfterDateQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.MessageIdsQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.TextBodyQuery
import com.scott.ezmessaging.extension.asUSPhoneNumber
import com.scott.ezmessaging.extension.getColumnValue
import com.scott.ezmessaging.extension.getCursor
import com.scott.ezmessaging.manager.DeviceManager
import com.scott.ezmessaging.model.Message.SmsMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * A content resolver responsible for communicating with the system's database of all SMS messages.
 */
internal class SmsContentResolver @Inject constructor(
    @ApplicationContext context: Context,
    private val deviceManager: DeviceManager
) {

    private val contentResolver: ContentResolver? = context.contentResolver

    private val columns = arrayOf(
        COLUMN_SMS_ID,
        COLUMN_SMS_THREAD_ID,
        COLUMN_SMS_ADDRESS,
        COLUMN_SMS_DATE_SENT,
        COLUMN_SMS_DATE_RECEIVED,
        COLUMN_SMS_HAS_BEEN_READ,
        COLUMN_SMS_BODY
    )

    /**
     * @return all messages that have been received.
     */
    fun getAllReceivedSmsMessages(): List<SmsMessage> {
        val messages = arrayListOf<SmsMessage>()
        contentResolver.getCursor(
            uri = Uri.parse(CONTENT_SMS_INBOX),
            columnsToReturn = columns
        )?.let { cursor ->
            while (cursor.moveToNext()) {
                runCatching {
                    val messageId = cursor.getColumnValue(COLUMN_SMS_ID)
                    val threadId = cursor.getColumnValue(COLUMN_SMS_THREAD_ID)
                    val address = cursor.getColumnValue(COLUMN_SMS_ADDRESS)
                    val dateSent = cursor.getColumnValue(COLUMN_SMS_DATE_SENT)
                    val dateReceived = cursor.getColumnValue(COLUMN_SMS_DATE_RECEIVED)
                    val beenRead = cursor.getColumnValue(COLUMN_SMS_HAS_BEEN_READ)
                    val body = cursor.getColumnValue(COLUMN_SMS_BODY)

                    // I don't care about messages that don't have all the required info so force unwrapping.
                    val message = SmsMessage(
                        messageId = messageId!!,
                        threadId = threadId!!,
                        senderAddress = address.asUSPhoneNumber()!!,
                        text = body!!,
                        dateSent = dateSent!!.toLong(),
                        dateReceived = dateReceived!!.toLong(),
                        hasBeenRead = beenRead == "1",
                        participants = setOf(address.asUSPhoneNumber()!!, deviceManager.getThisDeviceMainNumber())
                    )
                    messages.add(message)
                }.onFailure { logError(it) }
            }
        }
        return messages
    }

    /**
     * @return all messages that have been sent.
     */
    fun getAllSentSmsMessages(): List<SmsMessage> {
        val messages = arrayListOf<SmsMessage>()
        contentResolver.getCursor(
            uri = Uri.parse(CONTENT_SMS_OUTBOX),
            columnsToReturn = columns
        )?.let { cursor ->
            while (cursor.moveToNext()) {
                runCatching {
                    val messageId = cursor.getColumnValue(COLUMN_SMS_ID)
                    val threadId = cursor.getColumnValue(COLUMN_SMS_THREAD_ID)
                    val sentToAddress = cursor.getColumnValue(COLUMN_SMS_ADDRESS)
                    val dateSent = cursor.getColumnValue(COLUMN_SMS_DATE_RECEIVED) // date sent is the same as received with outgoing messages
                    val dateReceived = cursor.getColumnValue(COLUMN_SMS_DATE_RECEIVED)
                    val beenRead = cursor.getColumnValue(COLUMN_SMS_HAS_BEEN_READ)
                    val body = cursor.getColumnValue(COLUMN_SMS_BODY)

                    // I don't care about messages that don't have all the required info so forcing unwrapping.
                    val myAddress = deviceManager.getThisDeviceMainNumber()
                    val message = SmsMessage(
                        messageId = messageId!!,
                        threadId = threadId!!,
                        senderAddress = myAddress,
                        text = body!!,
                        dateSent = dateSent!!.toLong(),
                        dateReceived = dateReceived!!.toLong(),
                        hasBeenRead = beenRead == "1",
                        participants = setOf(sentToAddress.asUSPhoneNumber()!!, deviceManager.getThisDeviceMainNumber())
                    )
                    messages.add(message)
                }.onFailure { logError(it) }
            }
        }
        return messages
    }

    /**
     * @return a list of messages that match the provided params, if they exist.
     * @param text The text content of the message.
     * @param afterDateMillis returns all messages after the date.
     */
    fun findMessages(
        text: String? = null,
        afterDateMillis: Long? = null
    ): List<SmsMessage> {
        val outboxMessages = findMessages(CONTENT_SMS_OUTBOX, text, afterDateMillis)
        val inboxMessages = findMessages(CONTENT_SMS_INBOX, text, afterDateMillis)
        return outboxMessages + inboxMessages
    }

    /**
     * Inserts a message.
     * The system handles creating all of the other details about the message (e.g. hasBeenRead, messageId, threadId, etc.)
     * @return the inserted message. null, if the insert fails.
     */
    fun insertSentMessage(address: String, body: String): SmsMessage? {
        var insertedMessage: SmsMessage? = null
        runCatching {
            val dateSent = System.currentTimeMillis()
            contentResolver?.insert(Uri.parse(CONTENT_SMS_OUTBOX), ContentValues().apply {
                put(COLUMN_SMS_BODY, body)
                put(COLUMN_SMS_ADDRESS, address)
                put(COLUMN_SMS_DATE_SENT, dateSent)
            })
            insertedMessage = findMessages(CONTENT_SMS_OUTBOX, body, dateSent).first()
        }.onFailure { logError(it) }
        return insertedMessage
    }

    /**
     * Inserts a received message.
     * The system handles creating all of the other details about the message (e.g. threadId, messageId, etc.)
     * @return the inserted message. null, if the insert fails.
     */
    fun insertReceivedMessage(address: String, body: String, dateSent: Long, dateReceived: Long): SmsMessage? {
        var insertedMessage: SmsMessage? = null
        runCatching {
            contentResolver?.insert(Uri.parse(CONTENT_SMS_INBOX), ContentValues().apply {
                put(COLUMN_SMS_BODY, body)
                put(COLUMN_SMS_ADDRESS, address)
                put(COLUMN_SMS_DATE_RECEIVED, dateReceived)
                put(COLUMN_SMS_DATE_SENT, dateSent)
            })
            insertedMessage = findMessages(CONTENT_SMS_INBOX, body, dateReceived).first()
        }.onFailure { logError(it) }
        return insertedMessage
    }

    /**
     * Marks a message with the provided [messageId] as read.
     * @return true if the message was successfully updated.
     */
    fun markMessageAsRead(messageId: String): Boolean {
        val updates = ContentValues().apply {
            put(COLUMN_SMS_HAS_BEEN_READ, 1)
        }
        return updateMessages(messageId, CONTENT_SMS_INBOX, updates)
    }

    /**
     * Marks a message with the provided [messageId] as delivered.
     * @return true if the message was successfully updated.
     */
    fun markMessageAsDelivered(messageId: String): Boolean {
        val updates = ContentValues().apply {
            put(COLUMN_SMS_DELIVERY_DATE, System.currentTimeMillis())
        }
        return updateMessages(messageId, CONTENT_SMS_OUTBOX, updates)
    }

    /**
     * Deletes a message with the provided [messageId].
     * @return true if the message was successfully deleted.
     */
    fun deleteMessage(messageId: String): Boolean {
        runCatching {
            val query = MessageQueryBuilder()
                .addQuery(MessageIdsQuery(ids = setOf(messageId), columnName = COLUMN_SMS_ID))
                .build()
            contentResolver?.delete(Uri.parse(CONTENT_SMS), query, null)
            return true
        }.onFailure { logError(it) }
        return false
    }

    private fun findMessages(
        uri: String,
        text: String? = null,
        dateInMillis: Long? = null
    ): List<SmsMessage> {
        val messages = arrayListOf<SmsMessage>()
        val columnsFilter = MessageQueryBuilder()
            .addQuery(TextBodyQuery(text = text, columnName = COLUMN_SMS_BODY))
            .addQuery(AfterDateQuery(dateMillis = dateInMillis, columnName = COLUMN_SMS_DATE_RECEIVED))
            .build()

        // If the filters are null, just return. Otherwise, it'll return everything.
        if (columnsFilter.isEmpty()) return messages
        contentResolver.getCursor(
            uri = Uri.parse(uri),
            columnsToReturn = columns,
            columnsFilter
        )?.let { cursor ->
            while (cursor.moveToNext()) {
                runCatching {
                    val messageId = cursor.getColumnValue(COLUMN_SMS_ID)
                    val threadId = cursor.getColumnValue(COLUMN_SMS_THREAD_ID)
                    val sentToAddress = cursor.getColumnValue(COLUMN_SMS_ADDRESS)
                    val dateSent = cursor.getColumnValue(COLUMN_SMS_DATE_SENT)
                    val dateReceived = cursor.getColumnValue(COLUMN_SMS_DATE_RECEIVED)
                    val beenRead = cursor.getColumnValue(COLUMN_SMS_HAS_BEEN_READ)
                    val text = cursor.getColumnValue(COLUMN_SMS_BODY)

                    // I don't care about messages that don't have all the required info so forcing unwrapping.
                    val myAddress = deviceManager.getThisDeviceMainNumber()
                    val message = SmsMessage(
                        messageId = messageId!!,
                        threadId = threadId!!,
                        senderAddress = myAddress,
                        text = text!!,
                        dateSent = dateSent!!.toLong(),
                        dateReceived = dateReceived!!.toLong(),
                        hasBeenRead = beenRead == "1",
                        participants = setOf(sentToAddress.asUSPhoneNumber()!!, deviceManager.getThisDeviceMainNumber())
                    )
                    messages.add(message)
                }.onFailure { logError(it) }
            }
        }
        return messages
    }

    /**
     * Updates messages with the provided [messageId] with the [contentValues].
     * @return true if the message was successfully updated.
     */
    private fun updateMessages(messageId: String, uri: String, contentValues: ContentValues): Boolean {
        runCatching {
            contentResolver?.update(Uri.parse(uri), contentValues, "$COLUMN_SMS_ID=$messageId", null)
            return true
        }.onFailure { logError(it) }
        return false
    }

    private fun logError(t: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(
                SmsContentResolver::class.simpleName,
                "An sms content resolver error occurred: $t"
            )
        }
    }

    companion object {
        // Content URIs
        private const val CONTENT_SMS = "content://sms"
        private const val CONTENT_SMS_INBOX = "$CONTENT_SMS/inbox"
        private const val CONTENT_SMS_OUTBOX = "$CONTENT_SMS/sent"

        // Various columns a content resolver may return
        private const val COLUMN_SMS_ID = "_id"
        private const val COLUMN_SMS_THREAD_ID = "thread_id"
        private const val COLUMN_SMS_ADDRESS = "address"
        private const val COLUMN_SMS_DATE_SENT = "date_sent"
        private const val COLUMN_SMS_DATE_RECEIVED = "date" // If this was an outgoing message (coming from me), this represents the sent time.
        private const val COLUMN_SMS_HAS_BEEN_READ = "read"
        private const val COLUMN_SMS_DELIVERY_DATE = "delivery_date"
        private const val COLUMN_SMS_BODY = "body"
    }
}