package com.scott.ezmessaging.extension

/**
 * Converts a date in seconds to milliseconds. If the date is already in milliseconds, does nothing.
 */
internal fun String?.convertDateToEpochMilliseconds(): Long? {
    return try {
        if (this?.length == 13) {
            this.toLong()
        } else {
            this!!.toLong() * 1000
        }
    } catch (e: Exception) {
        null
    }
}