package com.scott.ezmessaging.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.MessageReceiveResult
import com.scott.ezmessaging.provider.DispatcherProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@AndroidEntryPoint
abstract class MessageReceivedBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var contentManager: ContentManager

    /**
     * Invoked when the new message has been received and inserted into the database.
     */
    abstract fun onMessageReceived(message: Message)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            contentManager.receiveMessage(intent) { receiveResult ->
                if (receiveResult is MessageReceiveResult.Success) {
                    receiveResult.messages.forEach {
                        onMessageReceived(it)
                    }
                }
            }
        }
    }
}