package com.scott.app.receiver

import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.receiver.MessageReceivedBroadcastReceiver

internal class SmsReceiver: MessageReceivedBroadcastReceiver() {

    override fun onMessageReceived(message: Message) {
        println("testingg on sms message received: $message")
    }
}