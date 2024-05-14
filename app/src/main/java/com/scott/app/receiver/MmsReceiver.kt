package com.scott.app.receiver

import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.receiver.MmsReceivedBroadcastReceiver

internal class MmsReceiver: MmsReceivedBroadcastReceiver() {

    override fun onMessageReceived(message: Message) {
        println("testingg on mms message received: $message")
    }
}