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
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(UnconfinedCoroutineRule::class)
class ContactManagerTest {

    private val context = mockk<Context>()
    private val subscriptionManager = mockk<SubscriptionManager>()
    private val subscriptionInfo = mockk<SubscriptionInfo>()
    private val sharedPreferencesManager = mockk<SharedPreferencesManager>(relaxed = true)
    private val contactManager = ContactManager(context, sharedPreferencesManager)

    @Test
    fun `device manager state is Uninitialized when created`() = runTest {
        contactManager.initializedState.test {
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
        contactManager.initialize()

        // Then
        contactManager.initializedState.test {
            awaitItem().shouldBe(Initializable.Initialized(Unit))
        }
    }

    @Test
    fun `inserts numbers into the shared preferences`() = runTest {
        // Given
        every { context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) } returns subscriptionManager
        val subscriptionInfo2 = mockk<SubscriptionInfo>()
        every { subscriptionInfo2.number } returns "1111111111"
        every { subscriptionInfo.number } returns "5555555555"
        every { subscriptionManager.activeSubscriptionInfoList } returns listOf(subscriptionInfo, subscriptionInfo2)

        // When
        contactManager.initialize()

        // Then
        verify { sharedPreferencesManager.setThisDeviceMainNumber("5555555555") }
        verify { sharedPreferencesManager.setAllDeviceNumbers(listOf("5555555555", "1111111111")) }
    }

    @Test
    fun `device manager state is Error if numbers don't exist`() = runTest {
        // Given
        every { context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) } returns subscriptionManager
        every { subscriptionManager.activeSubscriptionInfoList } returns listOf()

        // When
        shouldThrow<IllegalStateException> { contactManager.initialize() }

        // Then
        contactManager.initializedState.test {
            awaitItem().shouldBeInstanceOf<Initializable.Error>()
        }
    }

    @Test
    fun `getThisDeviceNumbers returns device numbers from shared preferences`() = runTest {
        // Given
        every { sharedPreferencesManager.getAllDeviceNumbers() } returns listOf("5555555555", "1111111111")

        // When
        val numbers = contactManager.getThisDeviceNumbers()

        // Then
        numbers.shouldBe(listOf("5555555555", "1111111111"))
    }

    @Test
    fun `getThisDeviceMainNumber returns number from shared preferences`() = runTest {
        // Given
        every { sharedPreferencesManager.getThisDeviceMainNumber() } returns "5555555555"

        // When
        val mainNumber = contactManager.getThisDeviceMainNumber()

        // Then
        mainNumber.shouldBe("5555555555")
    }
}