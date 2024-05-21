package com.scott.ezmessaging.extension

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class StringExtensionsTest {

    companion object {
        @JvmStatic
        private fun provideAsUSPhoneNumber(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("1234567890", "1234567890"),
                Arguments.of("+11234567890", "1234567890"),
                Arguments.of("+1 (123) 456-7890", "1234567890"),
                Arguments.of("garbage", null),
                Arguments.of("123456", "123456"),
                Arguments.of("", null),
                Arguments.of(null, null)
            )
        }

        @JvmStatic
        private fun provideConvertDateToMilliseconds(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("notANumber", null),
                Arguments.of("1716250865000", 1716250865000L),
                Arguments.of("1716250865", 1716250865000L)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("provideAsUSPhoneNumber")
    fun `asUSPhoneNumber formats as expected`(input: String?, expected: String?) {
        input.asUSPhoneNumber().shouldBe(expected)
    }

    @ParameterizedTest
    @MethodSource("provideConvertDateToMilliseconds")
    fun `convertDateToMilliseconds returns as expected`(input: String?, expected: Long?) {
        input.convertDateToEpochMilliseconds().shouldBe(expected)
    }
}