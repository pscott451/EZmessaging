package com.scott.ezmessaging.manager

import android.Manifest
import android.content.Context
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresPermission
import com.scott.ezmessaging.extension.asUSPhoneNumber
import com.scott.ezmessaging.model.Initializable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds a list of this device's phone numbers. There may be more than one if the device has with multiple sim cards.
 * Must be initialized with a ComponentActivity.
 */
@Singleton
internal class DeviceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _initializedState = MutableStateFlow<Initializable<Unit>>(Initializable.Uninitialized)
    val initializedState = _initializedState.asStateFlow()

    private val thisDeviceNumbers = arrayListOf<String>()

    /**
     * Builds the list of device numbers. Once initialized, [getThisDeviceNumbers] and [getThisDeviceMainNumber] will
     * be safe to use.
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    @Throws(IllegalStateException::class)
    fun initialize() {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager

        val numbers = subscriptionManager?.activeSubscriptionInfoList?.mapNotNull { it.number.asUSPhoneNumber() }

        if (numbers.isNullOrEmpty()) {
            _initializedState.value = Initializable.Error(Throwable("No numbers found for this device"))
            throw IllegalStateException("No numbers found for this device")
        } else {
            thisDeviceNumbers.addAll(numbers)
            _initializedState.value = Initializable.Initialized(Unit)
        }
    }

    /**
     * @return A list containing all of this device's phone numbers.
     */
    fun getThisDeviceNumbers(): List<String> = thisDeviceNumbers

    /**
     * @return The main number of this device.
     */
    fun getThisDeviceMainNumber(): String = thisDeviceNumbers.first()
}