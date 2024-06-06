package com.scott.ezmessaging.manager

import android.Manifest
import android.content.Intent
import android.provider.Telephony
import android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION
import android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION
import androidx.annotation.RequiresPermission
import com.google.android.mms.ContentType
import com.scott.ezmessaging.extension.asUSPhoneNumber
import com.scott.ezmessaging.model.Initializable
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.Message.MmsMessage
import com.scott.ezmessaging.model.Message.SmsMessage
import com.scott.ezmessaging.model.MessageData
import com.scott.ezmessaging.model.MessageReceiveResult
import com.scott.ezmessaging.model.MessageSendResult
import com.scott.ezmessaging.provider.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    override fun receiveMessage(
        intent: Intent,
        onReceiveResult: (MessageReceiveResult) -> Unit
    ) {
        coroutineScope.launch {
            when {
                intent.isSms() -> receiveSmsMessage(intent) {
                    resumeOnMainThread { onReceiveResult(it) }
                }
                intent.isMms() -> receiveMmsMessage(intent)  {
                    resumeOnMainThread { onReceiveResult(it) }
                }
                else -> resumeOnMainThread {
                    onReceiveResult(MessageReceiveResult.Failed("The received intent was neither sms or mms"))
                }
            }
        }
    }

    override fun sendSmsMessage(
        address: String,
        text: String,
        onMessageCreated: (SmsMessage?) -> Unit,
        onSent: (MessageSendResult) -> Unit,
        onDelivered: (Boolean) -> Unit
    ) {
        coroutineScope.launch {
            smsManager.sendMessage(
                address = address,
                text = text,
                onMessageCreated = { resumeOnMainThread { onMessageCreated(it) } },
                onSent = { resumeOnMainThread { onSent(it) } },
                onDelivered = { resumeOnMainThread { onDelivered(it) } }
            )
        }
    }

    override fun sendMmsMessage(
        message: MessageData,
        recipients: Array<String>,
        onMessageCreated: (MmsMessage?) -> Unit,
        onSent: (MessageSendResult) -> Unit
    ) {
        coroutineScope.launch {
            mmsManager.sendMessage(
                message = message,
                recipients = recipients,
                onMessageCreated = { resumeOnMainThread { onMessageCreated(it) } },
                onSent = { resumeOnMainThread { onSent(it) } }
            )
        }
    }

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
        exactText: String?,
        containsText: String?,
        afterDateMillis: Long?,
    ): List<Message>  = suspendCoroutine { continuation ->
        coroutineScope.launch {
            val mmsMessages = async { mmsManager.findMessages(exactText = exactText, containsText = containsText, afterDateMillis = afterDateMillis) }
            val smsMessages = async { smsManager.findMessages(exactText = exactText, containsText = containsText, afterDateMillis = afterDateMillis) }
            continuation.resume(mmsMessages.await() + smsMessages.await())
        }
    }

    private fun receiveSmsMessage(
        intent: Intent,
        onReceiveResult: (MessageReceiveResult) -> Unit
    ) {
        val list = arrayListOf<SmsMessage>()
        val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (message in smsMessages) {
            message.originatingAddress.asUSPhoneNumber()?.let { address ->
                smsManager.receiveMessage(
                    address = address,
                    body = message.messageBody,
                    dateSent = message.timestampMillis,
                    dateReceived = System.currentTimeMillis()
                )?.let { list.add(it) }
            }
        }
        if (list.isNotEmpty()) {
            onReceiveResult(MessageReceiveResult.Success(list))
        } else {
            onReceiveResult(MessageReceiveResult.Failed("Failed to insert the messages into the database"))
        }
    }

    private fun receiveMmsMessage(
        intent: Intent,
        onReceiveResult: (MessageReceiveResult) -> Unit
    ) = mmsManager.receiveMessage(intent, onReceiveResult)

    private fun markSmsMessageAsRead(messageId: String) = smsManager.markMessageAsRead(messageId)

    private fun markMmsMessageAsRead(messageId: String) = mmsManager.markMessageAsRead(messageId)

    private fun deleteSmsMessage(messageId: String) = smsManager.deleteMessage(messageId)

    private fun deleteMmsMessage(messageId: String) = mmsManager.deleteMessage(messageId)

    private fun Intent.isSms() = action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION

    private fun Intent.isMms(): Boolean {
        val isWAPAction = action == WAP_PUSH_DELIVER_ACTION || action == WAP_PUSH_RECEIVED_ACTION
        return isWAPAction && type == ContentType.MMS_MESSAGE
    }

    private fun resumeOnMainThread(action: () -> Unit) {
        coroutineScope.launch(Dispatchers.Main) { action() }
    }
}