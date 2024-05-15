package com.scott.ezmessaging.receiver

import com.scott.ezmessaging.model.MessageSendResult

internal object SmsSendCallbacks {
    val sentCallbacks = mutableMapOf<Long, (MessageSendResult) -> Unit>()
    val deliveredCallbacks = mutableMapOf<Long, (Boolean) -> Unit>()
}

internal object MmsSendCallbacks {
    val sentCallbacks = mutableMapOf<Long, (MessageSendResult) -> Unit>()
    val deliveredCallbacks = mutableMapOf<Long, (Boolean) -> Unit>()
}