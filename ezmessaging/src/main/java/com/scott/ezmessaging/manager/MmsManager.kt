package com.scott.ezmessaging.manager

import android.content.Intent
import android.util.Log
import com.scott.ezmessaging.BuildConfig
import com.scott.ezmessaging.contentresolver.MmsContentResolver
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.Message.MmsMessage
import com.scott.ezmessaging.model.MessageData
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
     * @param text The text content of the message.
     * @param afterDateMillis returns all messages after the date.
     */
    fun findMessages(
        text: String? = null,
        afterDateMillis: Long? = null
    ): List<Message> = mmsContentResolver.findMessages(text = text, afterDateMillis = afterDateMillis)

    /**
     * Handles receiving a message from another device.
     */
    suspend fun receiveMessage(intent: Intent) = suspendCoroutine { continuation ->
        googleManager.parseReceivedMmsIntent(intent) { processResult ->
            when (processResult) {
                is GoogleManager.ProcessResult.ProcessFailed -> {
                    if (BuildConfig.DEBUG) {
                        Log.e(MmsManager::class.simpleName, "An error occurred receiving the sms message: ${processResult.errorMessage}")
                    }
                    continuation.resume(null)
                }

                is GoogleManager.ProcessResult.ProcessSuccess -> {
                    continuation.resume(mmsContentResolver.findMessageByUri(processResult.saveLocation))
                }
            }
        }
    }

    /**
     * Sends an MMS message to another device and inserts the value into the content resolver.
     */
    suspend fun sendMessage(
        message: MessageData,
        recipients: Array<String>
    ): MmsMessage? = suspendCoroutine { continuation ->
        googleManager.sendMmsMessage(message, deviceManager.getThisDeviceMainNumber(), recipients) { processResult ->
            if (processResult is GoogleManager.ProcessResult.ProcessSuccess) {
                val message = mmsContentResolver.findMessageByUri(processResult.saveLocation)
                continuation.resume(message)
            } else {
                continuation.resume(null)
            }
        }
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
