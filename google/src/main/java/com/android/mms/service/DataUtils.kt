package com.android.mms.service

import android.content.Context
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.util.Log

object DataUtils {

    /**
     * @return true if mobile data is enabled.
     */
    @JvmStatic
    fun mobileDataEnabled(context: Context): Boolean? {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val c = Class.forName(connectivityManager.javaClass.name)
            val m = c.getDeclaredMethod("getMobileDataEnabled")
            m.isAccessible = true
            m.invoke(connectivityManager) as Boolean
        } catch (e: Exception) {
            Log.e(DataUtils::class.simpleName, "Unable to determine if data is enabled", e)
            null
        }
    }

    /**
     * @return true if mms is permitted over wifi.
     */
    @JvmStatic
    fun mmsOverWifiEnabled(context: Context?): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("mms_over_wifi", false)
    }
}