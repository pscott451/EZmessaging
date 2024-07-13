package com.scott.ezmessaging.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SharedPreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun setThisDeviceMainNumber(number: String) {
        with(sharedPreferences.edit()) {
            putString(DEVICE_MAIN_NUMBER, number)
            apply()
        }
    }

    fun setAllDeviceNumbers(numbers: List<String>) {
        var s = ""
        numbers.forEachIndexed { i, value ->
            s += if (i == numbers.lastIndex) value else "$value,"
        }
        with(sharedPreferences.edit()) {
            putString(DEVICE_ALL_NUMBERS, s)
            apply()
        }
    }

    fun getThisDeviceMainNumber() = getString(DEVICE_MAIN_NUMBER)

    fun getAllDeviceNumbers(): List<String> {
        val allDevices = getString(DEVICE_ALL_NUMBERS)
        return if (allDevices.isEmpty()) {
            emptyList()
        } else {
            allDevices.split(",")
        }
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    private fun getString(key: String, default: String = ""): String {
        return sharedPreferences.getString(key, default) ?: default
    }

    companion object {
        private const val PREFERENCES_NAME = "com.scott.ezmessaging.shared_preferences"

        // Keys
        private const val DEVICE_MAIN_NUMBER = "device_main_number"
        private const val DEVICE_ALL_NUMBERS = "device_all_numbers"
    }
}