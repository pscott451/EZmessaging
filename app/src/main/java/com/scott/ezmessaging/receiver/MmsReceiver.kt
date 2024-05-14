package com.scott.ezmessaging.receiver

import com.scott.ezmessaging.model.Message

internal class MmsReceiver: MmsReceivedBroadcastReceiver() {

    override fun onMessageReceived(message: Message) {
        println("testingg on mms message received: $message")
    }
}