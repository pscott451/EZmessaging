package com.scott.ezmessaging.model

import android.net.Uri

/**
 * A sealed interface indicating the google process result of an MMS message.
 */
sealed interface GoogleProcessResult {
    /**
     * An error occurred processing the message.
     * @property errorMessage the message indicating the error.
     */
    data class ProcessFailed(val errorMessage: String) : GoogleProcessResult

    /**
     * The message was successfully processed.
     */
    data class ProcessSuccess(val saveLocation: Uri) : GoogleProcessResult
}