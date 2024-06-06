package com.scott.ezmessaging.receiver

internal object SmsSendCallbacks {
    val sentCallbacks = mutableMapOf<Long, (Boolean) -> Unit>()
    val deliveredCallbacks = mutableMapOf<Long, (Boolean) -> Unit>()
}

internal object MmsSendCallbacks {
    val sentCallbacks = mutableMapOf<Long, (Boolean) -> Unit>()
    val deliveredCallbacks = mutableMapOf<Long, (Boolean) -> Unit>()
}