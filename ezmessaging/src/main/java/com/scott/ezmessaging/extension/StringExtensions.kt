package com.scott.ezmessaging.extension

/**
 * Converts a string to a phone number.
 * If the string contains any other characters other than the 10 digits that matter, removes those characters.
 * (e.g. "+1 (555) 555-5555" will format as "5555555555"
 * Returns null if the formatted string is empty.
 */
internal fun String?.asUSPhoneNumber(): String? {
    val stripped = this.toString().replace("\\D".toRegex(), "")
    val lastTen = stripped.takeLast(10)
    return lastTen.ifEmpty { null }
}

internal fun String?.convertDateToMilliseconds(): Long? {
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