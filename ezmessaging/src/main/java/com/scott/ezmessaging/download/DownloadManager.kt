package com.scott.ezmessaging.download

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.android.mms.MmsConfig
import com.scott.ezmessaging.download.DownloadManager.DownloadResult.DownloadError
import com.scott.ezmessaging.download.DownloadManager.DownloadResult.DownloadSuccess
import com.scott.ezmessaging.receiver.MmsFileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Downloads a multimedia message and broadcasts the result to an instance of a [DownloadReceiver].
     */
    fun downloadMultimediaMessage(
        downloadLocation: String,
        uri: Uri,
        subscriptionId: Int,
        onComplete: (downloadResult: DownloadResult) -> Unit
    ) {
        val receiver = DownloadReceiver()

        onDownloadCompleteListeners[receiver.action] = onComplete

        ContextCompat.registerReceiver(context, receiver, IntentFilter(receiver.action), ContextCompat.RECEIVER_NOT_EXPORTED)

        val fileName = "download.${System.currentTimeMillis()}.dat"
        val mDownloadFile = File(context.cacheDir, fileName)

        val contentUri = Uri.Builder()
            .authority(MmsFileProvider.getAuthority(context))
            .path(fileName)
            .scheme(ContentResolver.SCHEME_CONTENT)
            .build()

        val download = Intent(receiver.action).apply {
            putExtra(EXTRA_FILE_PATH, mDownloadFile.path)
            putExtra(EXTRA_LOCATION_URL, downloadLocation)
            putExtra(EXTRA_TRIGGER_PUSH, true)
            putExtra(EXTRA_URI, uri)
            putExtra(EXTRA_SUBSCRIPTION_ID, subscriptionId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, download, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smsManager = context.getSystemService(SmsManager::class.java)

        val configOverrides = Bundle().apply {
            MmsConfig.getHttpParams().takeIf { it.isNotEmpty() }?.let {
                putString(SmsManager.MMS_CONFIG_HTTP_PARAMS, it)
            }
        }

        grantUriPermission(context, contentUri)
        acquireWakeLock(context)
        smsManager.downloadMultimediaMessage(context, downloadLocation, contentUri, configOverrides, pendingIntent)
    }

    private fun acquireWakeLock(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "smsmms:download-mms-lock")
        wakeLock?.acquire((60 * 1000).toLong())
    }

    private fun grantUriPermission(context: Context, contentUri: Uri) {
        context.grantUriPermission(
            MmsFileProvider.getAuthority(context),
            contentUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    /**
     * Receives a broadcast when a multimedia message has been downloaded.
     */
    private class DownloadReceiver : BroadcastReceiver() {

        /**
         * The action used when registering this receiver.
         * Should be null in the case multiple mms messages have been received.
         */
        val action = "${ACTION_PREFIX}_${System.currentTimeMillis()}"

        override fun onReceive(context: Context?, intent: Intent?) {
            val httpStatus = intent?.getIntExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, 0) ?: 0
            val invalidHttp = httpStatus == 400 || httpStatus == 404
            if (context != null && intent != null && !invalidHttp && resultCode == Activity.RESULT_OK) {
                onDownloadCompleteListeners[action]?.invoke(DownloadSuccess(intent))
            } else {
                onDownloadCompleteListeners[action]?.invoke(DownloadError(httpStatus, resultCode))
            }
            onDownloadCompleteListeners.remove(action)
            context?.unregisterReceiver(this)
        }

        companion object {
            private const val ACTION_PREFIX = "com.scott.ezmessaging.download.DownloadManager\$DownloadReceiver."
        }
    }

    sealed interface DownloadResult {
        /**
         * @property httpStatus The http status from the failed download.
         * @property resultCode The intent result code from the failed download.
         */
        data class DownloadError(val httpStatus: Int, val resultCode: Int) : DownloadResult

        /**
         * @property intent The intent received from the successful download.
         */
        data class DownloadSuccess(val intent: Intent) : DownloadResult
    }

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_LOCATION_URL = "location_url"
        const val EXTRA_TRIGGER_PUSH = "trigger_push"
        const val EXTRA_URI = "notification_ind_uri"
        const val EXTRA_SUBSCRIPTION_ID = "subscription_id"

        private var onDownloadCompleteListeners = mutableMapOf<String, ((downloadResult: DownloadResult) -> Unit)>()
        private var wakeLock: WakeLock? = null
    }
}