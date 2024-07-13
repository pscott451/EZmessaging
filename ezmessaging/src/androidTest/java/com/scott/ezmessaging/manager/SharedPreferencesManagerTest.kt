package com.scott.ezmessaging.manager

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesManagerTest {

    private val context = InstrumentationRegistry.getInstrumentation().context

    private val sharedPreferencesManager = SharedPreferencesManager(context)

    @After
    fun clearPreferences() {
        sharedPreferencesManager.clear()
    }

    @Test
    fun insertsMainNumber() {
        // Given
        val number = "1111111111"

        // When
        sharedPreferencesManager.setThisDeviceMainNumber(number)

        // Then
        sharedPreferencesManager.getThisDeviceMainNumber().shouldBe(number)
    }

    @Test
    fun insertingOneNumberIntoAllNumbersIsExpected() {
        // Given
        val numbers = listOf("1111111111")

        // When
        sharedPreferencesManager.setAllDeviceNumbers(numbers)

        // Then
        sharedPreferencesManager.getAllDeviceNumbers().shouldBe(numbers)
    }

    @Test
    fun insertingMultipleNumberIntoAllNumbersIsExpected() {
        // Given
        val numbers = listOf("1111111111", "2222222222")

        // When
        sharedPreferencesManager.setAllDeviceNumbers(numbers)

        // Then
        sharedPreferencesManager.getAllDeviceNumbers().shouldBe(numbers)
    }

    @Test
    fun returnsEmptyListIfAllNumbersDontExist() {
        sharedPreferencesManager.getAllDeviceNumbers().shouldBe(emptyList())
    }
}