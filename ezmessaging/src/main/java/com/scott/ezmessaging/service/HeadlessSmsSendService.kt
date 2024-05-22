package com.scott.ezmessaging.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * This seems to only be necessary to allow the system to set the app as a default messaging app.
 */
internal class HeadlessSmsSendService: Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}