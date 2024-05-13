package com.scott.ezmessaging.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.provider.DispatcherProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
abstract class MmsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var contentManager: ContentManager

    private val coroutineScope by lazy {
        CoroutineScope(dispatcherProvider.io())
    }

    /**
     * Invoked when the new message has been inserted into the database.
     */
    abstract fun onMessageReceived(message: Message)

    // Suppressing the error as the messageManager will check the validity of the intent.
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            coroutineScope.launch {
                val messages = contentManager.receiveMessage(intent)
                if (messages.isNotEmpty()) {
                    messages.forEach {
                        if (it != null) onMessageReceived(it)
                    }
                }
            }
        }
    }
}