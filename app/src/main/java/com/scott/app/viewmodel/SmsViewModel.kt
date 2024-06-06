package com.scott.app.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.scott.app.R
import com.scott.app.receiver.MessageReceiver
import com.scott.app.receiver.SmsReceiver
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.model.MessageSendResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SmsViewModel @Inject constructor(
    messageReceiver: MessageReceiver,
    @ApplicationContext private val context: Context,
    private val contentManager: ContentManager
): ViewModel() {

    private val _sendingInProgress = MutableStateFlow(false)
    val sendingInProgress = _sendingInProgress.asStateFlow()

    val receivedMessages = messageReceiver.smsMessages

    /**
     * Once this is successfully sent and delivered, the [MessageReceiver] receives the message
     * within the [SmsReceiver] which is then emitted via the [receivedMessages].
     */
    fun sendMessage(
        recipient: String,
        message: String
    ) {
        _sendingInProgress.value = true
        contentManager.sendSmsMessage(
            address = recipient,
            text = message,
            onMessageCreated = { },
            onSent = { sendResult ->
                if (sendResult is MessageSendResult.Failed) {
                    _sendingInProgress.value = false
                    makeToast(sendResult.errorMessage)
                }
            },
            onDelivered = { message ->
                _sendingInProgress.value = false
                message?.let { makeToast(context.getString(R.string.sendSms_messagedelivered)) }
            }
        )
    }

    private fun makeToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}