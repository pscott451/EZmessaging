package com.scott.ezmessaging.receiver

import com.scott.ezmessaging.model.Message

internal class SmsReceiver: SmsReceivedBroadcastReceiver() {

    override fun onMessageReceived(message: Message) {
        println("testingg on sms message received: $message")
    }
}