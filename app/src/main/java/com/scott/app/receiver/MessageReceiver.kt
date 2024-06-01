package com.scott.app.receiver

import com.scott.ezmessaging.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageReceiver @Inject constructor() {

    private val _smsMessages = MutableStateFlow<List<Message.SmsMessage>>(emptyList())
    val smsMessages = _smsMessages.asStateFlow()

    private val _mmsMessages = MutableStateFlow<List<Message.MmsMessage>>(emptyList())
    val mmsMessages = _mmsMessages.asStateFlow()

    fun receiveMessage(message: Message) {
        when (message) {
            is Message.MmsMessage -> updateMms(message)
            is Message.SmsMessage -> updateSms(message)
        }
    }

    private fun updateMms(message: Message.MmsMessage) {
        _mmsMessages.value += listOf(message)
    }

    private fun updateSms(message: Message.SmsMessage) {
        _smsMessages.value += listOf(message)
    }
}