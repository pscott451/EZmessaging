package com.scott.ezmessaging.extension

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class StringExtensionsTest {

    companion object {
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
    @MethodSource("provideConvertDateToMilliseconds")
    fun `convertDateToMilliseconds returns as expected`(input: String?, expected: Long?) {
        input.convertDateToEpochMilliseconds().shouldBe(expected)
    }
}