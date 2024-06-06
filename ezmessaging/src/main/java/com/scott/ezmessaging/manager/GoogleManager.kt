package com.scott.ezmessaging.manager

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import com.android.mms.dom.smil.parser.SmilXmlSerializer
import com.android.mms.service.DownloadRequest
import com.android.mms.service.MmsConfig
import com.android.mms.util.RateController
import com.google.android.mms.ContentType
import com.google.android.mms.MMSPart
import com.google.android.mms.MmsException
import com.google.android.mms.pdu_google.CharacterSets
import com.google.android.mms.pdu_google.DeliveryInd
import com.google.android.mms.pdu_google.EncodedStringValue
import com.google.android.mms.pdu_google.GenericPdu
import com.google.android.mms.pdu_google.NotificationInd
import com.google.android.mms.pdu_google.PduBody
import com.google.android.mms.pdu_google.PduComposer
import com.google.android.mms.pdu_google.PduHeaders
import com.google.android.mms.pdu_google.PduParser
import com.google.android.mms.pdu_google.PduPart
import com.google.android.mms.pdu_google.PduPersister
import com.google.android.mms.pdu_google.ReadOrigInd
import com.google.android.mms.pdu_google.SendReq
import com.google.android.mms.smil.SmilHelper
import com.google.android.mms.util.SqliteWrapper
import com.scott.ezmessaging.download.DownloadManager
import com.scott.ezmessaging.download.DownloadManager.DownloadResult.DownloadError
import com.scott.ezmessaging.download.DownloadManager.DownloadResult.DownloadSuccess
import com.scott.ezmessaging.extension.getColumnValue
import com.scott.ezmessaging.extension.getCursor
import com.scott.ezmessaging.manager.ContentManager.SupportedMessageTypes.CONTENT_TYPE_TEXT
import com.scott.ezmessaging.manager.ContentManager.SupportedMessageTypes.isValidMessageType
import com.scott.ezmessaging.model.GoogleProcessResult
import com.scott.ezmessaging.model.MessageData
import com.scott.ezmessaging.receiver.MmsFileProvider
import com.scott.ezmessaging.receiver.MmsSentBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * A manager responsible for everything surrounding the google module.
 * There's a lot of complicated logic here, so isolating it in one place.
 */
internal class GoogleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager,
) {

    init {
        com.android.mms.MmsConfig.init(context)
        RateController.init(context)
        com.android.mms.util.DownloadManager.init(context)
    }

    /**
     * Handles parsing the mms intent once received.
     */
    fun parseReceivedMmsIntent(
        intent: Intent,
        processResult: (GoogleProcessResult) -> Unit
    ) {
        val pushData = intent.getByteArrayExtra("data")
        val pdu = PduParser(pushData).parse()

        if (pdu == null) {
            processResult(GoogleProcessResult.ProcessFailed("Invalid PUSH data"))
            return
        }

        val persister = PduPersister.getPduPersister(context)
        val messageType = pdu.messageType
        val subscriptionID = intent.getIntExtra("subscription", SmsManager.getDefaultSmsSubscriptionId())

        try {
            when (messageType) {
                PduHeaders.MESSAGE_TYPE_DELIVERY_IND,
                PduHeaders.MESSAGE_TYPE_READ_ORIG_IND -> {
                    // The message was delivered or read. Update the database to reflect this.
                    findThreadId(context, pdu, messageType)?.let { threadId ->
                        val uri = persister.persist(
                            pdu, Uri.parse("content://mms/inbox"), true,
                            true, null, subscriptionID, false
                        );
                        // Update thread ID for ReadOrigInd & DeliveryInd.
                        val values = ContentValues(1).apply {
                            put(Telephony.Mms.THREAD_ID, threadId)
                        }
                        context.contentResolver.update(
                            uri, values, null, null
                        )
                        processResult(GoogleProcessResult.ProcessSuccess(uri))
                    } ?: run {
                        processResult(GoogleProcessResult.ProcessFailed("Thread ID not found"))
                    }
                }

                PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND -> {
                    // A new message...
                    (pdu as? NotificationInd)?.let { notificationInd ->
                        val configOverrides = context.getSystemService(SmsManager::class.java).carrierConfigValues
                        val appendTransactionId = configOverrides.getBoolean(SmsManager.MMS_CONFIG_APPEND_TRANSACTION_ID)

                        if (com.android.mms.MmsConfig.getTransIdEnabled() || appendTransactionId) {
                            val contentLocation = notificationInd.contentLocation
                            if ('='.code.toByte() == contentLocation[contentLocation.size - 1]) {
                                val transactionId = notificationInd.transactionId
                                val contentLocationWithId = ByteArray(
                                    contentLocation.size + transactionId.size
                                )
                                System.arraycopy(
                                    contentLocation, 0, contentLocationWithId,
                                    0, contentLocation.size
                                )
                                System.arraycopy(
                                    transactionId, 0, contentLocationWithId,
                                    contentLocation.size, transactionId.size
                                )
                                notificationInd.contentLocation = contentLocationWithId
                            }
                        }

                        // Save the MMS message.
                        val contentUri = persister.persist(
                            pdu, Telephony.Mms.Inbox.CONTENT_URI,
                            false,
                            true,
                            null,
                            subscriptionID,
                            false
                        )

                        val downloadLocation = try {
                            getContentDownloadLocation(context, contentUri)
                        } catch (ex: MmsException) {
                            persister.getContentLocationFromPduHeader(pdu)
                        }

                        downloadManager.downloadMultimediaMessage(
                            downloadLocation ?: "",
                            contentUri,
                            subscriptionID
                        ) { downloadResult ->
                            when (downloadResult) {
                                is DownloadError -> {
                                    val errorMessage = "Download failed. Result code: ${downloadResult.resultCode}, HTTP Status: ${downloadResult.httpStatus}"
                                    processResult(GoogleProcessResult.ProcessFailed(errorMessage))
                                }

                                is DownloadSuccess -> {
                                    // Finished downloading, add to the database.
                                    with(downloadResult.intent) {
                                        val path = getStringExtra(DownloadManager.EXTRA_FILE_PATH)
                                        val subscriptionId = getIntExtra(DownloadManager.EXTRA_SUBSCRIPTION_ID, SmsManager.getDefaultSmsSubscriptionId())
                                        val locationUrl = getStringExtra(DownloadManager.EXTRA_LOCATION_URL)
                                        persistDownloadToDatabase(
                                            context,
                                            path,
                                            subscriptionId,
                                            locationUrl ?: "",
                                            processResult
                                        )
                                    }
                                }
                            }
                        }
                    } ?: run {
                        processResult(GoogleProcessResult.ProcessFailed("Invalid PDU"))
                    }
                }
            }
        } catch (e: MmsException) {
            processResult(GoogleProcessResult.ProcessFailed("An Mms Exception Occurred. Type was: $messageType"))
        } catch (e: Exception) {
            processResult(GoogleProcessResult.ProcessFailed("An unknown error occurred"))
        }
    }

    fun sendMmsMessage(
        message: MessageData,
        fromAddress: String,
        recipients: Array<String>,
        onInsertedIntoDatabase: (Uri?) -> Unit,
        onSent: (Uri?, Exception?) -> Unit
    ) {
        try {
            val mmsPart = when (message) {
                is MessageData.ContentUri -> {
                    var newMessageData: MessageData.Image? = null
                    context.contentResolver.getCursor(message.uri)?.let { cursor ->
                        while (cursor.moveToNext()) {
                            cursor.getColumnValue("mime_type")?.let { mimeType ->
                                context.contentResolver.openInputStream(message.uri)?.let { inputStream ->
                                    val image = BitmapFactory.decodeStream(inputStream)
                                    newMessageData = MessageData.Image(image, mimeType)
                                }
                            }
                        }
                    }
                    newMessageData?.let {
                        sendMmsMessage(it, fromAddress, recipients, onInsertedIntoDatabase, onSent)
                    } ?: run {
                        onInsertedIntoDatabase(null)
                        onSent(null, Exception("Failed to decode the image from the provided uri"))
                    }
                    return
                }
                is MessageData.Image -> {
                    var compressQuality = 100
                    var imageByteArray: ByteArray?
                    do {
                        // keep compressing the bitmap until it's an acceptable size
                        imageByteArray = bitmapToByteArray(message.bitmap, compressQuality)
                        compressQuality -= 10
                    } while (imageByteArray != null && imageByteArray.size > ONE_MEGA_BYTE && compressQuality >= 0)

                    val isValidType = message.mimeType.isValidMessageType()
                    when {
                        imageByteArray == null -> {
                            onInsertedIntoDatabase(null)
                            onSent(null, Exception("Failed to convert the bitmap to a byte array"))
                            return
                        }
                        imageByteArray.size > ONE_MEGA_BYTE -> {
                            onInsertedIntoDatabase(null)
                            onSent(null, Exception("Image too large to send. Size after max compression: ${imageByteArray.size}"))
                            return
                        }
                        !isValidType -> {
                            onInsertedIntoDatabase(null)
                            onSent(null, Exception("Invalid message type: ${message.mimeType}"))
                            return
                        }
                        else -> {
                            MMSPart().apply {
                                MimeType = message.mimeType
                                Name = "image_${System.currentTimeMillis()}"
                                Data = imageByteArray
                            }
                        }
                    }
                }

                is MessageData.Text -> {
                    if (message.text.isEmpty()) {
                        onInsertedIntoDatabase(null)
                        onSent(null, Exception("Message cannot be empty"))
                        return
                    }
                    MMSPart().apply {
                        MimeType = CONTENT_TYPE_TEXT
                        Name = "text"
                        Data = message.text.toByteArray()
                    }
                }
            }

            val sendReq = buildPdu(fromAddress, recipients, mmsPart)
            val fileName = "send.${System.currentTimeMillis()}.dat"
            val sendFile = File(context.cacheDir, fileName)

            // Save the message to the outbox.
            val locationUri = PduPersister.getPduPersister(context).persist(
                sendReq,
                Uri.parse("content://mms/outbox"),
                true,
                true,
                null,
                -1,
                false
            )

            onInsertedIntoDatabase(locationUri)

            val writerUri = Uri.Builder()
                .authority(MmsFileProvider.getAuthority(context))
                .path(fileName)
                .scheme(ContentResolver.SCHEME_CONTENT)
                .build()

            FileOutputStream(sendFile).apply {
                write(PduComposer(context, sendReq).make())
                close()
            }

            val configOverrides = Bundle().apply {
                putBoolean(
                    SmsManager.MMS_CONFIG_GROUP_MMS_ENABLED,
                    true
                )
                com.android.mms.MmsConfig.getHttpParams().takeIf { it.isNotEmpty() }?.let {
                    putString(SmsManager.MMS_CONFIG_HTTP_PARAMS, it)
                }
                putInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, com.android.mms.MmsConfig.getMaxMessageSize())
            }

            writerUri?.let {
                val sentIntent = MmsSentBroadcastReceiver().buildPendingIntent(context) { successfullySent ->
                    if (successfullySent) {
                        onSent(locationUri, null)
                        markTheMessageAsSent(context, locationUri)
                    }
                    sendFile.delete()
                }
                val smsManager = context.getSystemService(SmsManager::class.java)
                smsManager.sendMultimediaMessage(
                    context,
                    it, null, configOverrides, sentIntent
                )
            } ?: run {
                onSent(null, Exception("Writer URI was null"))
            }
        } catch (e: Exception) {
            onSent(null, Exception("An error occurred sending the mms message: ${e.message}"))
        }
    }

    private fun buildPdu(
        fromAddress: String?,
        recipients: Array<String>,
        part: MMSPart
    ): SendReq {
        return SendReq().apply {
            prepareFromAddress(fromAddress)
            recipients.forEach {
                addTo(EncodedStringValue(it))
            }
            date = System.currentTimeMillis() / 1000
            messageSize = 1
            messageClass = PduHeaders.MESSAGE_CLASS_PERSONAL_STR.toByteArray()
            expiry = (7 * 24 * 60 * 60).toLong()
            priority = PduHeaders.PRIORITY_NORMAL
            deliveryReport = PduHeaders.VALUE_NO
            readReport = PduHeaders.VALUE_NO
            body = PduBody().apply {
                addTextPart(this, part)
                val out = ByteArrayOutputStream()
                SmilXmlSerializer.serialize(SmilHelper.createSmilDocument(this), out)
                val smilPart = PduPart().apply {
                    setContentId("smil".toByteArray())
                    setContentLocation("smil.xml".toByteArray())
                    setContentType(ContentType.APP_SMIL.toByteArray())
                    setData(out.toByteArray())
                }
                addPart(0, smilPart)
            }
        }
    }

    private fun addTextPart(pb: PduBody, p: MMSPart): Int {
        val filename = p.Name
        val part = PduPart().apply {
            if (p.MimeType.startsWith("text")) {
                setCharset(CharacterSets.UTF_8)
            }
            setContentType(p.MimeType.toByteArray())
            setContentLocation(filename.toByteArray())
            filename.split(".").takeIf { it.isNotEmpty() }?.last()?.let {
                setContentId(it.toByteArray())
            }
            setData(p.Data)
        }
        pb.addPart(part)
        return part.getData().size
    }

    private fun bitmapToByteArray(image: Bitmap, compressQuality: Int): ByteArray? {
        val output: ByteArray?
        try {
            val stream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, compressQuality, stream)
            output = stream.toByteArray()
            stream.close()
        } catch (e: Exception) {
            return null
        }
        return output
    }

    /**
     * Persists the mms to the database once it's been downloaded.
     */
    private fun persistDownloadToDatabase(
        context: Context,
        path: String?,
        subscriptionID: Int,
        locationUrl: String,
        processResult: (GoogleProcessResult) -> Unit
    ) {
        try {
            val mDownloadFile = File(path!!)
            val nBytes = mDownloadFile.length().toInt()
            val reader = FileInputStream(mDownloadFile)
            val response = ByteArray(nBytes)
            reader.read(response, 0, nBytes)
            val uri = DownloadRequest.persist(
                context, response,
                MmsConfig.Overridden(MmsConfig(context), null),
                locationUrl,
                subscriptionID, null
            )
            mDownloadFile.delete()
            reader.close()
            processResult(GoogleProcessResult.ProcessSuccess(uri))
        } catch (e: FileNotFoundException) {
            processResult(GoogleProcessResult.ProcessFailed("MMS received, file not found exception"))
        } catch (e: IOException) {
            processResult(GoogleProcessResult.ProcessFailed("MMS received, io exception"))
        }
    }

    private fun getContentDownloadLocation(context: Context, uri: Uri): String? {
        context.contentResolver.getCursor(
            uri,
            arrayOf(Telephony.Mms.CONTENT_LOCATION, Telephony.Mms.LOCKED)
        )?.let { cursor ->
            if (cursor.count == 1 && cursor.moveToNext()) {
                val location = cursor.getString(0)
                cursor.close()
                return location
            }
        }
        // Couldn't find the location
        return null
    }

    private fun findThreadId(
        context: Context,
        pdu: GenericPdu,
        type: Int
    ): Long? {
        val byteArray = if (type == PduHeaders.MESSAGE_TYPE_DELIVERY_IND) {
            (pdu as DeliveryInd).messageId
        } else {
            (pdu as ReadOrigInd).messageId
        }
        val messageId = String(byteArray)

        val query =
            "(${Telephony.Mms.MESSAGE_ID}=${DatabaseUtils.sqlEscapeString(messageId)} AND ${Telephony.Mms.MESSAGE_TYPE}=${PduHeaders.MESSAGE_TYPE_SEND_REQ}"

        context.contentResolver.getCursor(
            Telephony.Mms.CONTENT_URI,
            arrayOf(Telephony.Mms.THREAD_ID),
            query
        )?.let { cursor ->
            if (cursor.count == 1 && cursor.moveToNext()) {
                val id = cursor.getLong(0)
                cursor.close()
                return id
            }
        }
        // Couldn't find the ID.
        return null
    }

    private fun markTheMessageAsSent(context: Context, uri: Uri) {
        val values = ContentValues(1)
        values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_SENT)
        SqliteWrapper.update(
            context, context.contentResolver, uri, values,
            null, null
        )
    }

    companion object {
        // The max size an MMS message can be. Anything greater will fail to send.
        private const val ONE_MEGA_BYTE = 1_000_000
    }
}
