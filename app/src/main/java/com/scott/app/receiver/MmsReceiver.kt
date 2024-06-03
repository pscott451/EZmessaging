package com.scott.app.receiver

import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.receiver.MessageReceivedBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class MmsReceiver: MessageReceivedBroadcastReceiver() {

    @Inject
    lateinit var messageReceiver: MessageReceiver

    override fun onMessageReceived(message: Message) {
        messageReceiver.receiveMessage(message)
    }
}