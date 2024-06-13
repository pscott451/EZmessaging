package com.scott.ezmessaging.contentresolver

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import com.scott.ezmessaging.BuildConfig
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.AfterDateQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.ContainsTextQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.ExactTextQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.MessageIdsQuery
import com.scott.ezmessaging.extension.asUSPhoneNumber
import com.scott.ezmessaging.extension.convertDateToEpochMilliseconds
import com.scott.ezmessaging.extension.getColumnValue
import com.scott.ezmessaging.extension.getCursor
import com.scott.ezmessaging.manager.ContentManager.SupportedMessageTypes.CONTENT_TYPE_TEXT
import com.scott.ezmessaging.manager.ContentManager.SupportedMessageTypes.isValidMessageType
import com.scott.ezmessaging.manager.DeviceManager
import com.scott.ezmessaging.model.Message.MmsMessage
import com.scott.ezmessaging.provider.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A content resolver responsible for communicating with the system's database of all MMS messages.
 */
internal class MmsContentResolver @Inject constructor(
    @ApplicationContext context: Context,
    dispatcherProvider: DispatcherProvider,
    private val deviceManager: DeviceManager
) {

    private val contentResolver: ContentResolver? = context.contentResolver
    private val coroutineScope = CoroutineScope(dispatcherProvider.io())

    // Keep track of the total number of valid messages to help calculate
    // the percent complete when retrieving all messages via getAllMmsMessages().
    private var numberOfValidMessages = 0f

    /**
     * Retrieves all existing MMS messages.
     */
    suspend fun getAllMmsMessages(
        percentContentRetrieved: ((Float) -> Unit)? = null,
        percentMessagesBuilt: ((Float) -> Unit)? = null,
    ): List<MmsMessage> = suspendCoroutine { continuation ->
        coroutineScope.launch {
            var percentMetadataComplete = 0f
            var percentAddressesComplete = 0f
            var percentContentComplete = 0f
            runCatching {
                val metadataMapDeferred = async {
                    getMessageMetadata {
                        percentMetadataComplete = it
                        percentContentRetrieved?.invoke((percentMetadataComplete + percentAddressesComplete + percentContentComplete) / 3)
                    }
                }
                val addressesMapDeferred = async {
                    getMessageAddresses {
                        percentAddressesComplete = it
                        percentContentRetrieved?.invoke((percentMetadataComplete + percentAddressesComplete + percentContentComplete) / 3)
                    }
                }
                val contentMapDeferred = async {
                    getMessageContent {
                        percentContentComplete = it
                        percentContentRetrieved?.invoke((percentMetadataComplete + percentAddressesComplete + percentContentComplete) / 3)
                    }
                }

                val metadataMap = metadataMapDeferred.await()
                val addressesMap = addressesMapDeferred.await()
                val contentMap = contentMapDeferred.await()
                buildMessages(metadataMap, contentMap, addressesMap, percentMessagesBuilt)
            }.onSuccess {
                continuation.resume(it)
            }.onFailure {
                logError(it)
                continuation.resume(emptyList())
            }
            numberOfValidMessages = 0f
        }
    }

    /**
     * @return a list of messages that match the provided params, if they exist.
     * @param messageIds A set of message ids. Matches the ids returned from the [COLUMN_MMS_ID] column.
     * @param exactText returns any messages that match the provided text exactly.
     * @param containsText returns any messages that contain the provided text.
     * @param afterDateMillis returns all messages after the date.
     */
    fun findMessages(
        messageIds: Set<String>? = null,
        exactText: String? = null,
        containsText: String? = null,
        afterDateMillis: Long? = null
    ): List<MmsMessage> {
        var messages = listOf<MmsMessage>()
        runCatching {
            val contentMap: Map<String, List<ContentMetadata>>
            val addressMap: Map<String, AddressMetadata>
            val metadataMap: Map<String, MessageMetadata>
            if (exactText == null && containsText == null && afterDateMillis != null) {
                // If searching by date, we need to get the metadata first since that's where the date columns live.
                val metadataFilters = buildMetadataQuery(
                    afterDateMillis = afterDateMillis,
                    messageIds = messageIds
                )
                metadataMap = getMessageMetadata(metadataFilters)

                val contentFilters = buildContentQuery(messageIds = metadataMap.keys)
                val addressFilters = buildAddressQuery(messageIds = metadataMap.keys)
                contentMap = getMessageContent(contentFilters)
                addressMap = getMessageAddresses(addressFilters)
            } else {
                val contentFilters = buildContentQuery(
                    exactText = exactText,
                    containsText = containsText,
                    messageIds = messageIds
                )
                contentMap = getMessageContent(contentFilters)

                val metadataFilters = buildMetadataQuery(messageIds = contentMap.keys, afterDateMillis = afterDateMillis)
                val addressFilters = buildAddressQuery(messageIds = contentMap.keys)
                metadataMap = getMessageMetadata(metadataFilters)
                addressMap = getMessageAddresses(addressFilters)
            }
            messages = buildMessages(metadataMap, contentMap, addressMap)
        }.onFailure {
            logError(it)
        }
        numberOfValidMessages = 0f
        return messages
    }

    /**
     * Finds a message by it's URI.
     * @param messageUri the exact location the message should reside.
     * @return The message if it exists. Otherwise, null
     */
    fun findMessageByUri(messageUri: Uri?): MmsMessage? {
        if (messageUri == null) return null
        runCatching {
            contentResolver.getCursor(
                messageUri,
                arrayOf(COLUMN_MMS_ID)
            )?.let {  cursor ->
                if (cursor.moveToNext()) {
                    val messages = arrayListOf<MmsMessage>()
                    cursor.getColumnValue(COLUMN_MMS_ID)?.let { id ->
                        messages.addAll(findMessages(messageIds = setOf(id)))
                    }
                    if (messages.isNotEmpty()) return messages.first()
                }
            }
        }
        return null
    }

    /**
     * Deletes a message with the provided [messageId].
     * @return true if the message was successfully deleted.
     */
    fun deleteMessage(messageId: String): Boolean {
        runCatching {
            // delete all message metadata
            contentResolver?.delete(Uri.parse(CONTENT_MMS_ALL), "$COLUMN_MMS_ID=\"$messageId\"", null)
            // delete all content metadata
            contentResolver?.delete(Uri.parse(CONTENT_MMS_BODY), "$COLUMN_MMS_MID=\"$messageId\"", null)
            // delete all address metadata
            contentResolver?.delete(Uri.parse(CONTENT_MMS_ADDRESS), "$COLUMN_MMS_MESSAGE_ID=\"$messageId\"", null)
            return true
        }.onFailure {
            logError(it)
        }
        return false
    }

    /**
     * Marks a message with the provided [messageId] as read.
     * @return true, if the message was successfully updated.
     */
    fun markMessageAsRead(messageId: String): Boolean {
        val updates = ContentValues().apply {
            put(COLUMN_MMS_HAS_BEEN_READ, 1)
        }
        return updateMessages(
            messageId = messageId,
            messageIdColumn = COLUMN_MMS_ID,
            uri = CONTENT_MMS_ALL,
            contentValues = updates
        )
    }

    /**
     * Updates messages with the provided [messageId] with the [contentValues].
     * @return true, if the message was successfully updated.
     */
    private fun updateMessages(
        messageId: String,
        messageIdColumn: String,
        uri: String,
        contentValues: ContentValues
    ): Boolean {
        runCatching {
            contentResolver?.update(Uri.parse(uri), contentValues, "$messageIdColumn=$messageId", null)
            return true
        }.onFailure { logError(it) }
        return false
    }

    /**
     * @return a map containing message's thread id, messageId, date sent, date received, and if it's been read.
     * @param columnFilters Any filters that should be applied when retrieving the message content.
     * If [columnFilters] is an empty string, returns an empty map.
     * If [columnFilters] is null, returns everything.
     */
    private fun getMessageMetadata(
        columnFilters: String? = null,
        percentComplete: ((Float) -> Unit)? = null
    ): Map<String, MessageMetadata> {
        val messageIdToMetaData = mutableMapOf<String, MessageMetadata>()
        if (columnFilters == "") return messageIdToMetaData
        contentResolver.getCursor(
            uri = CONTENT_MMS_ALL,
            columnsToReturn = arrayOf(
                COLUMN_MMS_ID,
                COLUMN_MMS_THREAD_ID,
                COLUMN_MMS_DATE_SENT,
                COLUMN_MMS_DATE,
                COLUMN_MMS_HAS_BEEN_READ
            ),
            columnFilters
        )?.let { cursor ->
            val messageCount = cursor.count.toFloat()
            var messagesLoaded = 0f
            while (cursor.moveToNext()) {
                val id = cursor.getColumnValue(COLUMN_MMS_ID)
                val threadId = cursor.getColumnValue(COLUMN_MMS_THREAD_ID)
                val dateSent = cursor.getColumnValue(COLUMN_MMS_DATE_SENT)
                val dateReceived = cursor.getColumnValue(COLUMN_MMS_DATE)
                val hasBeenRead = cursor.getColumnValue(COLUMN_MMS_HAS_BEEN_READ)
                val metaData = MessageMetadata(
                    threadId = threadId,
                    messageId = id,
                    dateSent = dateSent,
                    dateReceived = dateReceived,
                    hasBeenRead = hasBeenRead
                )
                id?.let {
                    messageIdToMetaData[it] = metaData
                }
                messagesLoaded++
                percentComplete?.invoke(messagesLoaded/messageCount)
            }
            percentComplete?.invoke(1f)
        }
        return messageIdToMetaData
    }

    /**
     * @return a map containing message's sender address and all participants.
     * @param columnFilters Any filters that should be applied when retrieving the message content.
     * If [columnFilters] is an empty string, returns an empty map.
     * If [columnFilters] is null, returns everything.
     */
    private fun getMessageAddresses(
        columnFilters: String? = null,
        percentComplete: ((Float) -> Unit)? = null
    ): Map<String, AddressMetadata> {
        val messageIdToAddresses = mutableMapOf<String, AddressMetadata>()
        if (columnFilters == "") return messageIdToAddresses
        contentResolver.getCursor(
            uri = CONTENT_MMS_ADDRESS,
            columnsToReturn = arrayOf(
                COLUMN_MMS_ADDRESS,
                COLUMN_MMS_PARTICIPANT_TYPE,
                COLUMN_MMS_MESSAGE_ID
            ),
            columnFilters
        )?.let { cursor ->
            val messageCount = cursor.count.toFloat()
            var messagesLoaded = 0f
            while (cursor.moveToNext()) {
                val msgId = cursor.getColumnValue(COLUMN_MMS_MESSAGE_ID)
                val address = cursor.getColumnValue(COLUMN_MMS_ADDRESS).asUSPhoneNumber()
                val senderAddress = if (cursor.getColumnValue(COLUMN_MMS_PARTICIPANT_TYPE) == PARTICIPANT_TYPE_FROM.toString()) address.asUSPhoneNumber() else null

                msgId?.let { id ->
                    messageIdToAddresses[id]?.let {
                        if (it.senderAddress == null) {
                            it.senderAddress = senderAddress
                        }
                        it.participants.add(address)
                    } ?: run {
                        messageIdToAddresses[id] = AddressMetadata(
                            senderAddress = senderAddress,
                            participants = mutableSetOf(address)
                        )
                    }
                }
                messagesLoaded++
                percentComplete?.invoke(messagesLoaded/messageCount)
            }
            percentComplete?.invoke(1f)
        }
        return messageIdToAddresses
    }

    /**
     * @return a map containing message's unique id, text, and content type.
     * @param columnFilters Any filters that should be applied when retrieving the message content.
     * If [columnFilters] is an empty string, returns an empty map.
     * If [columnFilters] is null, returns everything.
     */
    private fun getMessageContent(
        columnFilters: String? = null,
        percentComplete: ((Float) -> Unit)? = null
    ): Map<String, List<ContentMetadata>> {
        val messageIdToContent = mutableMapOf<String, ArrayList<ContentMetadata>>()
        if (columnFilters == "") return messageIdToContent
        contentResolver.getCursor(
            uri = CONTENT_MMS_BODY,
            columnsToReturn = arrayOf(
                COLUMN_MMS_TEXT,
                COLUMN_MMS_DATA,
                COLUMN_MMS_CT,
                COLUMN_MMS_MID,
                COLUMN_MMS_ID
            ),
            columnFilters
        )?.let { cursor ->
            val messageCount = cursor.count.toFloat()
            var messagesLoaded = 0f
            while (cursor.moveToNext()) {
                val mid = cursor.getColumnValue(COLUMN_MMS_MID)
                val uniqueId = cursor.getColumnValue(COLUMN_MMS_ID)
                val type = cursor.getColumnValue(COLUMN_MMS_CT)
                val text = cursor.getColumnValue(COLUMN_MMS_TEXT)
                val content = ContentMetadata(
                    uniqueId = uniqueId,
                    text = text,
                    type = type
                )

                if (mid != null && type.isValidMessageType()) {
                    messageIdToContent[mid]?.add(content) ?: run {
                        messageIdToContent[mid] = arrayListOf(content)
                    }
                    numberOfValidMessages++
                }
                messagesLoaded++
                percentComplete?.invoke(messagesLoaded/messageCount)
            }
            percentComplete?.invoke(1f)
        }
        return messageIdToContent
    }

    private fun buildMessages(
        metadataMap: Map<String, MessageMetadata>,
        contentMap: Map<String, List<ContentMetadata>>,
        addressesMap: Map<String, AddressMetadata>,
        percentMessagesBuilt: ((Float) -> Unit)? = null
    ): List<MmsMessage> {
        val messageDetails = mutableMapOf<String, ArrayList<MessageDetails>>()

        val totalCount = contentMap.size + metadataMap.size + addressesMap.size + numberOfValidMessages
        var numberProcessed = 0f
        contentMap.forEach { (msgId, contentMetadata) ->
            contentMetadata.forEach { content ->
                val details = MessageDetails(
                    messageId = msgId,
                    text = content.text,
                    messageType = content.type,
                    uniqueId = content.uniqueId
                )
                messageDetails[msgId]?.add(details) ?: run {
                    messageDetails[msgId] = arrayListOf(details)
                }
            }
            numberProcessed++
            percentMessagesBuilt?.invoke(numberProcessed / totalCount)
        }

        addressesMap.forEach { (msgId, addressMetadata) ->
            messageDetails[msgId]?.let {  detailsList ->
                detailsList.forEach {
                    it.senderAddress = addressMetadata.senderAddress
                    it.participants.addAll(addressMetadata.participants)
                }
            }
            numberProcessed++
            percentMessagesBuilt?.invoke(numberProcessed / totalCount)
        }

        metadataMap.forEach { (msgId, metadata) ->
            messageDetails[msgId]?.let { detailsList ->
                detailsList.forEach {
                    it.threadId = metadata.threadId
                    it.dateSent = metadata.dateSent
                    it.dateReceived = metadata.dateReceived
                    it.hasBeenRead = metadata.hasBeenRead
                }
            }
            numberProcessed++
            percentMessagesBuilt?.invoke(numberProcessed / totalCount)
        }

        val messages = arrayListOf<MmsMessage>()
        messageDetails.values.forEach { details ->
            messages.addAll(details.mapNotNull {
                numberProcessed++
                percentMessagesBuilt?.invoke(numberProcessed / totalCount)
                it.toMessage()
            })
        }
        percentMessagesBuilt?.invoke(1f)
        return messages
    }

    private fun buildMetadataQuery(
        afterDateMillis: Long? = null,
        messageIds: Set<String>? = null
    ) = MessageQueryBuilder()
        .addQuery(MessageIdsQuery(ids = messageIds, columnName = COLUMN_MMS_ID))
        .addQuery(AfterDateQuery(dateMillis = afterDateMillis, columnName = COLUMN_MMS_DATE))
        .build()

    private fun buildContentQuery(
        exactText: String? = null,
        containsText: String? = null,
        messageIds: Set<String>? = null
    ) = MessageQueryBuilder()
        .addQuery(ExactTextQuery(text = exactText, columnName = COLUMN_MMS_TEXT))
        .addQuery(ContainsTextQuery(text = containsText, columnName = COLUMN_MMS_TEXT))
        .addQuery(MessageIdsQuery(ids = messageIds, columnName = COLUMN_MMS_MID))
        .build()

    private fun buildAddressQuery(messageIds: Set<String>) = MessageQueryBuilder()
        .addQuery(MessageIdsQuery(ids = messageIds, columnName = COLUMN_MMS_MESSAGE_ID))
        .build()

    private fun MessageDetails.toMessage(): MmsMessage? {
        val threadId = threadId
        val messageId = messageId
        val uniqueId = uniqueId
        val senderAddress = senderAddress.asUSPhoneNumber()
        val dateSent = if (senderAddress == deviceManager.getThisDeviceMainNumber()) dateReceived else dateSent // date sent is the same as received if on this device
        val dateReceived = dateReceived
        val hasBeenRead = hasBeenRead
        val participants = participants.mapNotNull { it.asUSPhoneNumber() }.toSet()
        val text = text
        val messageType = messageType
        val hasImage = messageType != CONTENT_TYPE_TEXT
        val hasText = !text.isNullOrEmpty()
        val validContent = hasText || hasImage
        return if (validContent && uniqueId != null && threadId != null && messageId != null &&
            dateSent != null && dateReceived != null && hasBeenRead != null &&
            senderAddress != null && participants.isNotEmpty() && messageType != null
        ) {
            MmsMessage(
                uniqueId = uniqueId,
                messageId = messageId,
                threadId = threadId,
                senderAddress = senderAddress,
                text = text,
                // Some MMS messages are in seconds. If so, convert to milliseconds for consistency
                dateSent = dateSent.convertDateToEpochMilliseconds() ?: 0,
                dateReceived = dateReceived.convertDateToEpochMilliseconds() ?: 0,
                hasBeenRead = hasBeenRead == "1",
                hasImage = hasImage,
                messageType = messageType,
                participants = participants
            )
        } else {
            null
        }
    }

    private fun logError(t: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(
                SmsContentResolver::class.simpleName,
                "An mms content resolver error occurred: $t"
            )
        }
    }

    companion object {
        // Content URIs
        private const val CONTENT_MMS_ALL = "content://mms"
        private const val CONTENT_MMS_ADDRESS = "content://mms/addr"
        private const val CONTENT_MMS_BODY = "content://mms/part"

        // Various columns a content resolver may return
        private const val COLUMN_MMS_ID = "_id"
        private const val COLUMN_MMS_THREAD_ID = "thread_id"
        private const val COLUMN_MMS_MESSAGE_ID = "msg_id"
        private const val COLUMN_MMS_MID = "mid"
        private const val COLUMN_MMS_ADDRESS = "address"
        private const val COLUMN_MMS_DATE_SENT = "date_sent"
        private const val COLUMN_MMS_DATE = "date" // If this was an outgoing message (coming from me), this represents the sent time.
        private const val COLUMN_MMS_HAS_BEEN_READ = "read"
        private const val COLUMN_MMS_PARTICIPANT_TYPE = "type"
        private const val COLUMN_MMS_CONTENT_TYPE = "ct_t"
        private const val COLUMN_MMS_CT = "ct"
        private const val COLUMN_MMS_TEXT = "text"
        private const val COLUMN_MMS_DATA = "_data"

        // Each participant associated with a message is assigned a type.
        private const val PARTICIPANT_TYPE_FROM = 137
        private const val PARTICIPANT_TYPE_BCC = 129
        private const val PARTICIPANT_TYPE_CC = 130
        private const val PARTICIPANT_TYPE_TO = 151
    }

    private data class MessageDetails(
        var threadId: String? = null,
        var messageId: String? = null,
        var uniqueId: String? = null,
        var dateSent: String? = null,
        var dateReceived: String? = null,
        var hasBeenRead: String? = null,
        var senderAddress: String? = null,
        var participants: MutableSet<String?> = mutableSetOf(),
        var text: String? = null,
        var messageType: String? = null
    )

    private data class MessageMetadata(
        val threadId: String?,
        val messageId: String?,
        var dateSent: String?,
        var dateReceived: String?,
        val hasBeenRead: String?
    )

    private data class AddressMetadata(
        var senderAddress: String?,
        var participants: MutableSet<String?> = mutableSetOf()
    )

    private data class ContentMetadata(
        var uniqueId: String?,
        var text: String?,
        var type: String?
    )
}