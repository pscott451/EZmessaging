package com.scott.app.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scott.app.receiver.MessageReceiver
import com.scott.app.receiver.MmsReceiver
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.MessageData
import com.scott.ezmessaging.model.MessageSendResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MmsViewModel @Inject constructor(
    messageReceiver: MessageReceiver,
    @ApplicationContext private val context: Context,
    private val contentManager: ContentManager
) : ViewModel() {

    private val _sendingInProgress = MutableStateFlow(false)
    val sendingInProgress = _sendingInProgress.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<Message.MmsMessage>>(emptyList())
    val receivedMessages = _receivedMessages.asStateFlow()

    init {
        messageReceiver.mmsMessages.onEach {
            _receivedMessages.value = it
            _sendingInProgress.value = false
        }.launchIn(viewModelScope)
    }

    /**
     * Once this is successfully sent, the [MessageReceiver] receives the message
     * within the [MmsReceiver] which is then emitted via the [receivedMessages].
     */
    fun sendMessage(
        messageData: MessageData,
        recipients: Array<String>
    ) {
        _sendingInProgress.value = true
        contentManager.sendMmsMessage(
            message = messageData,
            recipients = recipients,
            onSent = { sendResult ->
                if (sendResult is MessageSendResult.Failed) {
                    _sendingInProgress.value = false
                    makeToast(sendResult.errorMessage)
                }
            }
        )
    }

    private fun makeToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}