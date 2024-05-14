package com.scott.ezmessaging.manager

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
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
import com.scott.ezmessaging.extension.getCursor
import com.scott.ezmessaging.manager.ContentManager.SupportedMessageTypes.CONTENT_TYPE_TEXT
import com.scott.ezmessaging.manager.ContentManager.SupportedMessageTypes.isValidMessageType
import com.scott.ezmessaging.model.MessageData
import com.scott.ezmessaging.receiver.MmsFileProvider
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
        processResult: (ProcessResult) -> Unit
    ) {
        val pushData = intent.getByteArrayExtra("data")
        val pdu = PduParser(pushData).parse()

        if (pdu == null) {
            processResult(ProcessResult.ProcessFailed("Invalid PUSH data"))
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
                            true, null, subscriptionID
                        );
                        // Update thread ID for ReadOrigInd & DeliveryInd.
                        val values = ContentValues(1).apply {
                            put(Telephony.Mms.THREAD_ID, threadId)
                        }
                        context.contentResolver.update(
                            uri, values, null, null
                        )
                        processResult(ProcessResult.ProcessSuccess(uri))
                    } ?: run {
                        processResult(ProcessResult.ProcessFailed("Thread ID not found"))
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
                            subscriptionID
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
                                    val errorMessage =
                                        "Download failed. Result code: ${downloadResult.resultCode}, HTTP Status: ${downloadResult.httpStatus}"
                                    processResult(ProcessResult.ProcessFailed(errorMessage))
                                }

                                is DownloadSuccess -> {
                                    // Finished downloading, add to the database.
                                    with(downloadResult.intent) {
                                        val path = getStringExtra(DownloadManager.EXTRA_FILE_PATH)
                                        val subscriptionId =
                                            getIntExtra(DownloadManager.EXTRA_SUBSCRIPTION_ID, SmsManager.getDefaultSmsSubscriptionId())
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
                        processResult(ProcessResult.ProcessFailed("Invalid PDU"))
                    }
                }
            }
        } catch (e: MmsException) {
            processResult(ProcessResult.ProcessFailed("An Mms Exception Occurred. Type was: $messageType"))
        } catch (e: Exception) {
            processResult(ProcessResult.ProcessFailed("An unknown error occurred"))
        }
    }

    fun sendMmsMessage(
        message: MessageData,
        fromAddress: String,
        recipients: Array<String>,
        sentResult: (ProcessResult) -> Unit
    ) {
        val callbackId = System.currentTimeMillis()
        try {
            val sendResultCallbackObject = SendResultCallbackObject(sentResult)
            sendMessageCallbacks[callbackId] = sendResultCallbackObject
            val mmsPart = when (message) {
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
                            invokeSentResultCallback(context, callbackId, ProcessResult.ProcessFailed("Failed to convert the bitmap to a byte array"))
                            return
                        }
                        imageByteArray.size > ONE_MEGA_BYTE -> {
                            invokeSentResultCallback(context, callbackId, ProcessResult.ProcessFailed("Image too large to send. Size after max compression: ${imageByteArray.size}"))
                            return
                        }
                        !isValidType -> {
                            invokeSentResultCallback(context, callbackId, ProcessResult.ProcessFailed("Invalid message type: ${message.mimeType}"))
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
                        invokeSentResultCallback(context, callbackId, ProcessResult.ProcessFailed("Message cannot be empty"))
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
                -1
            )

            sendResultCallbackObject.apply {
                cachedFile = sendFile
                saveLocation = locationUri
            }

            val intent = Intent(MmsSent.MMS_SENT).apply {
                putExtra(EXTRA_CALLBACK_ID, callbackId)
            }

            ContextCompat.registerReceiver(context, MmsSent(), IntentFilter(MmsSent.MMS_SENT), ContextCompat.RECEIVER_NOT_EXPORTED)

            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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
                val smsManager = context.getSystemService(SmsManager::class.java)
                smsManager.sendMultimediaMessage(
                    context,
                    it, null, configOverrides, pendingIntent
                )
            } ?: run {
                invokeSentResultCallback(context, callbackId, ProcessResult.ProcessFailed("Writer URI was null"))
                pendingIntent.send(SmsManager.MMS_ERROR_IO_ERROR)
            }
        } catch (e: Exception) {
            invokeSentResultCallback(context, callbackId, ProcessResult.ProcessFailed("An error occurred sending the mms message: ${e.message}"))
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
            date = System.currentTimeMillis()
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
        processResult: (ProcessResult) -> Unit
    ) {
        try {
            val mDownloadFile = File(path!!)
            val nBytes = mDownloadFile.length().toInt()
            val reader = FileInputStream(mDownloadFile)
            val response = ByteArray(nBytes)
            reader.read(response, 0, nBytes)
            //val tasks: List<CommonAsyncTask> = getNotificationTask(context, intent, response)
            val uri = DownloadRequest.persist(
                context, response,
                MmsConfig.Overridden(MmsConfig(context), null),
                locationUrl,
                subscriptionID, null
            )
            mDownloadFile.delete()
            reader.close()
            // TODO do I need an ACK task?
            /*if (tasks != null) {
                Log.v(MmsReceivedReceiver.TAG, "running the common async notifier for download")
                for (task in tasks) task.executeOnExecutor(MmsReceivedReceiver.RECEIVE_NOTIFICATION_EXECUTOR)
            }*/
            processResult(ProcessResult.ProcessSuccess(uri))
        } catch (e: FileNotFoundException) {
            processResult(ProcessResult.ProcessFailed("MMS received, file not found exception"))
        } catch (e: IOException) {
            processResult(ProcessResult.ProcessFailed("MMS received, io exception"))
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

    /**
     * A sealed interface representing the process status.
     */
    sealed interface ProcessResult {
        /**
         * An error occurred processing the message.
         * @property errorMessage the message indicating the error.
         */
        data class ProcessFailed(val errorMessage: String) : ProcessResult

        /**
         * Successfully processed the mms message.
         */
        data class ProcessSuccess(val saveLocation: Uri) : ProcessResult
    }

    private data class SendResultCallbackObject(
        val callback: (ProcessResult) -> Unit,
        var cachedFile: File? = null,
        var saveLocation: Uri? = null
    )

    private class MmsSent: BroadcastReceiver() {

        companion object {
            const val MMS_SENT = "com.scott.ezmessaging.manager.GoogleManager\$MMS_SENT"
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                val sendResultCallbackId = intent.getLongExtra(EXTRA_CALLBACK_ID, -1)
                if (resultCode == Activity.RESULT_OK) {
                    sendMessageCallbacks[sendResultCallbackId]?.saveLocation?.let { saveLocation ->
                        invokeSentResultCallback(context, sendResultCallbackId, ProcessResult.ProcessSuccess(saveLocation))
                    }
                } else {
                    invokeSentResultCallback(
                        context,
                        sendResultCallbackId,
                        ProcessResult.ProcessFailed("An error occurred sending the message. Result Code: $resultCode")
                    )
                }
                context.unregisterReceiver(this)
            }
        }
    }

    companion object {
        private val sendMessageCallbacks = mutableMapOf<Long, SendResultCallbackObject>()

        private fun invokeSentResultCallback(
            context: Context,
            id: Long,
            result: ProcessResult
        ) {
            sendMessageCallbacks[id]?.let { callbackObject ->
                callbackObject.callback.invoke(result)
                callbackObject.cachedFile?.delete()
                callbackObject.saveLocation?.let { markTheMessageAsSent(context, it) }
            }
            sendMessageCallbacks.remove(id)
        }

        private fun markTheMessageAsSent(context: Context, uri: Uri) {
            val values = ContentValues(1)
            values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_SENT)
            SqliteWrapper.update(
                context, context.contentResolver, uri, values,
                null, null
            )
        }

        private const val EXTRA_CALLBACK_ID = "ExtraCallBackId"
        private const val ONE_MEGA_BYTE = 1_000_000
    }
}
