package com.scott.ezmessaging.model

sealed class Initializable<out T> {
    object Uninitialized : Initializable<Nothing>()
    data class Initialized<T>(val data: T) : Initializable<T>()
    data class Error(val throwable: Throwable) : Initializable<Nothing>()
}