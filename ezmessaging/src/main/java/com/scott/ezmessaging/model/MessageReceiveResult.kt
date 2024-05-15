package com.scott.ezmessaging.model

/**
 * A sealed interface indicating the result of receiving a message.
 */
sealed interface MessageReceiveResult {
    /**
     * An error occurred receiving the message.
     * @property errorMessage the message indicating the error.
     */
    data class Failed(val errorMessage: String) : MessageReceiveResult

    /**
     * The message was successfully received.
     * @property messages the messages that were received.
     */
    data class Success(val messages: List<Message>) : MessageReceiveResult
}