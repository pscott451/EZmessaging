package com.scott.app.receiver

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.scott.app.BuildConfig
import com.scott.ezmessaging.model.Message
import com.scott.ezmessaging.receiver.MessageReceivedBroadcastReceiver
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
internal class SmsReceiver: MessageReceivedBroadcastReceiver() {

    @Inject
    lateinit var messageReceiver: MessageReceiver

    override fun onMessageReceived(message: Message) {
        logCat.info(message = "testingg message: $message")
        messageReceiver.receiveMessage(message)
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