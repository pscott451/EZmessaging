package com.scott.ezmessaging.receiver

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

internal class MmsSentBroadcastReceiver: BroadcastReceiver() {

    private val action = "${ACTION_PREFIX}_${System.currentTimeMillis()}"

    fun buildPendingIntent(context: Context, onSent: (Boolean) -> Unit): PendingIntent {
        val callbackId = System.currentTimeMillis()
        MmsSendCallbacks.sentCallbacks[callbackId] = onSent
        ContextCompat.registerReceiver(context, this, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
        val intent = Intent(action).apply {
            putExtra(EXTRA_CALLBACK_ID, callbackId)
        }
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val sendResultCallbackId = intent.getLongExtra(EXTRA_CALLBACK_ID, -1)
            MmsSendCallbacks.sentCallbacks[sendResultCallbackId] ?.invoke(resultCode == Activity.RESULT_OK)
            MmsSendCallbacks.sentCallbacks.remove(sendResultCallbackId)
            context.unregisterReceiver(this)
        }
    }

    companion object {
        private const val ACTION_PREFIX = "com.scott.ezmessaging.manager.MMS_SENT"
        private const val EXTRA_CALLBACK_ID = "ExtraCallBackId"
    }
}