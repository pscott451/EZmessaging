package com.scott.ezmessaging.model

/**
 * A sealed interface indicating the send result of an MMS message.
 */
sealed interface MessageSendResult {
    /**
     * An error occurred sending the message.
     * @property errorMessage the message indicating the error.
     */
    data class Failed(val errorMessage: String) : MessageSendResult

    /**
     * The message was successfully sent.
     */
    object Success: MessageSendResult
}