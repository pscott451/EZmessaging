package com.scott.ezmessaging.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.scott.ezmessaging.BuildConfig
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.model.MessageReceiveResult
import com.scott.ezmessaging.provider.DispatcherProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
abstract class MessageReceivedBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var contentManager: ContentManager

    @Inject
    lateinit var logCat: LogCat

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

@Singleton
class LogCat @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val loggingEnabled = BuildConfig.DEBUG

    fun debug(tag: String?, message: String?, throwable: Throwable? = null) {
        if (loggingEnabled){
            writeToFile(tag, message)
            Log.d(tag, message, throwable)
        }
    }

    fun info(tag: String? = null, message: String?, throwable: Throwable? = null) {
        if (loggingEnabled){
            writeToFile(tag, message)
            Log.i(tag, message, throwable)
        }
    }

    fun warn(tag: String?, message: String?, throwable: Throwable? = null) {
        if (loggingEnabled){
            writeToFile(tag, message)
            Log.w(tag, message, throwable)
        }
    }

    fun error(tag: String?, message: String?, throwable: Throwable? = null) {
        if (loggingEnabled){
            writeToFile(tag, message)
            Log.e(tag, message, throwable)
        }
    }

    fun verbose(tag: String?, message: String?, throwable: Throwable? = null) {
        if (loggingEnabled){
            writeToFile(tag, message)
            Log.v(tag, message, throwable)
        }
    }

    fun wtf(tag: String?, message: String?, throwable: Throwable? = null) {
        if (loggingEnabled){
            writeToFile(tag, message)
            Log.wtf(tag, message, throwable)
        }
    }

    private fun writeToFile(tag: String?, message: String?) {
        try {
            val documentsDir = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)}/nasmessenger/")
            if (!documentsDir.exists()) documentsDir.mkdir()
            val logsFile = File("$documentsDir/logs.txt")

            // If the above file is manually deleted, this is required to be able to recreate it.
            MediaScannerConnection.scanFile(context, arrayOf(logsFile.toString()), arrayOf("text/plain"), null)

            val fw = FileWriter(logsFile.absoluteFile, true)
            val bw = BufferedWriter(fw)

            val beautified = message.asJson()?.let { "\n$it" } ?: run { message }
            bw.write(":$tag ----> $beautified\n\n\n" + "")
            bw.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    private fun String?.asJson(): String? {
        return try {
            JSONObject(this).toString(2).replace("\\", "")
        } catch (e: Exception) {
            try {
                JSONArray(this).toString(2).replace("\\", "")
            } catch (e: Exception) {
                null
            }
        }
    }
}