package com.scott.ezmessaging.manager

import android.Manifest
import android.content.Intent
import android.provider.Telephony
import android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION
import android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.mms.ContentType
import com.scott.ezmessaging.BuildConfig
import com.scott.ezmessaging.extension.asUSPhoneNumber
import com.scott.ezmessaging.model.Initializable
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.Message.MmsMessage
import com.scott.ezmessaging.model.Message.SmsMessage
import com.scott.ezmessaging.model.MessageData
import com.scott.ezmessaging.provider.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ContentManagerImpl(
    private val smsManager: SmsManager,
    private val mmsManager: MmsManager,
    private val deviceManager: DeviceManager,
    dispatcherProvider: DispatcherProvider,
) : ContentManager {

    private val coroutineScope = CoroutineScope(dispatcherProvider.io())

    private val _initializedState = MutableStateFlow<Initializable<Unit>>(Initializable.Uninitialized)
    override val initializedState = _initializedState.asStateFlow()

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    override fun initialize() {
        deviceManager.initializedState.onEach {
            _initializedState.value = it
        }.launchIn(coroutineScope)
        deviceManager.initialize()
    }

    override suspend fun getAllMessages() = suspendCoroutine { continuation ->
        coroutineScope.launch {
            val smsMessagesDeferred = async { smsManager.getAllMessages() }
            val mmsMessagesDeferred = async { mmsManager.getAllMessages() }
            continuation.resume(smsMessagesDeferred.await() + mmsMessagesDeferred.await())
        }
    }

    override suspend fun receiveMessage(intent: Intent) =
        when {
            intent.isSms() -> receiveSmsMessage(intent)
            intent.isMms() -> receiveMmsMessage(intent)
            else -> {
                if (BuildConfig.DEBUG) {
                    Log.e(ContentManagerImpl::class.simpleName, "Received an unknown message type.")
                }
                emptyList()
            }
        }

    override fun sendSmsMessage(
        address: String,
        text: String,
        onSent: (Boolean) -> Unit,
        onDelivered: (Boolean) -> Unit
    ) {
        smsManager.sendMessage(address, text, onSent, onDelivered)
    }

    override suspend fun sendMmsMessage(
        message: MessageData,
        recipients: Array<String>
    ) = mmsManager.sendMessage(message, recipients)

    override fun markMessageAsRead(message: Message) =
        when (message) {
            is SmsMessage -> markSmsMessageAsRead(message.messageId)
            is MmsMessage -> markMmsMessageAsRead(message.messageId)
        }

    override fun deleteMessage(message: Message) =
        when (message) {
            is SmsMessage -> deleteSmsMessage(message.messageId)
            is MmsMessage -> deleteMmsMessage(message.messageId)
        }

    override suspend fun getMessagesByParams(
        text: String?,
        afterDateMillis: Long?
    ): List<Message> {
        val mmsMessages = mmsManager.findMessages(text = text, afterDateMillis = afterDateMillis)
        val smsMessages = smsManager.findMessages(text = text, afterDateMillis = afterDateMillis)
        return mmsMessages + smsMessages
    }

    private fun receiveSmsMessage(intent: Intent): List<SmsMessage?> {
        val list = arrayListOf<SmsMessage?>()
        val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (message in smsMessages) {
            message.originatingAddress.asUSPhoneNumber()?.let { address ->
                list.add(
                    smsManager.receiveMessage(
                        address = address,
                        body = message.messageBody,
                        dateSent = message.timestampMillis,
                        dateReceived = System.currentTimeMillis()
                    )
                )
            }
        }
        return list
    }

    private suspend fun receiveMmsMessage(intent: Intent): List<MmsMessage?> {
        return listOf(mmsManager.receiveMessage(intent))
    }

    private fun markSmsMessageAsRead(messageId: String) = smsManager.markMessageAsRead(messageId)

    private fun markMmsMessageAsRead(messageId: String) = mmsManager.markMessageAsRead(messageId)

    private fun deleteSmsMessage(messageId: String) = smsManager.deleteMessage(messageId)

    private fun deleteMmsMessage(messageId: String) = mmsManager.deleteMessage(messageId)

    private fun Intent.isSms() = action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION

    private fun Intent.isMms(): Boolean {
        val isWAPAction = action == WAP_PUSH_DELIVER_ACTION || action == WAP_PUSH_RECEIVED_ACTION
        return isWAPAction && type == ContentType.MMS_MESSAGE
    }
}