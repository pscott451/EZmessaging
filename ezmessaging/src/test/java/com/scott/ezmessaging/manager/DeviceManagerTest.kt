package com.scott.ezmessaging.manager

import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import app.cash.turbine.test
import com.scott.ezmessaging.UnconfinedCoroutineRule
import com.scott.ezmessaging.model.Initializable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(UnconfinedCoroutineRule::class)
class DeviceManagerTest {

    private val context = mockk<Context>()
    private val subscriptionManager = mockk<SubscriptionManager>()
    private val subscriptionInfo = mockk<SubscriptionInfo>()
    private val deviceManager = DeviceManager(context)

    @Test
    fun `device manager state is Uninitialized when created`() = runTest {
        deviceManager.initializedState.test {
            awaitItem().shouldBe(Initializable.Uninitialized)
        }
    }

    @Test
    fun `device manager state is Initialized if numbers exist`() = runTest {
        // Given
        every { context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) } returns subscriptionManager
        every { subscriptionInfo.number } returns "5555555555"
        every { subscriptionManager.activeSubscriptionInfoList } returns listOf(subscriptionInfo)

        // When
        deviceManager.initialize()

        // Then
        deviceManager.initializedState.test {
            awaitItem().shouldBe(Initializable.Initialized(Unit))
        }
    }

    @Test
    fun `device manager state is Error if numbers don't exist`() = runTest {
        // Given
        every { context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) } returns subscriptionManager
        every { subscriptionManager.activeSubscriptionInfoList } returns listOf()

        // When
        shouldThrow<IllegalStateException> { deviceManager.initialize() }

        // Then
        deviceManager.initializedState.test {
            awaitItem().shouldBeInstanceOf<Initializable.Error>()
        }
    }

    @Test
    fun `getThisDeviceNumbers returns device numbers`() = runTest {
        // Given
        every { context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) } returns subscriptionManager
        every { subscriptionInfo.number } returns "5555555555"
        every { subscriptionManager.activeSubscriptionInfoList } returns listOf(subscriptionInfo)

        // When
        deviceManager.initialize()
        val numbers = deviceManager.getThisDeviceNumbers()

        // Then
        numbers.shouldBe(listOf("5555555555"))
    }

    @Test
    fun `getThisDeviceMainNumber returns first number in list of device numbers`() = runTest {
        // Given
        every { context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) } returns subscriptionManager
        every { subscriptionInfo.number } returns "5555555555"
        val subscriptionInfo2 = mockk<SubscriptionInfo>()
        every { subscriptionInfo2.number } returns "1111111111"
        every { subscriptionManager.activeSubscriptionInfoList } returns listOf(subscriptionInfo, subscriptionInfo2)

        // When
        deviceManager.initialize()
        val mainNumber = deviceManager.getThisDeviceMainNumber()

        // Then
        mainNumber.shouldBe("5555555555")
    }
}