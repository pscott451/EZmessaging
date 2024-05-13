package com.scott.ezmessaging.contentresolver

import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.AfterDateQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.MessageIdsQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.TextBodyQuery
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class MessageQueryBuilderTest {

    companion object {
        @JvmStatic
        private fun provideQueries(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(TextBodyQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """text="Hello""""),
                Arguments.of(TextBodyQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """text="Hello" AND date>=1"""),
                Arguments.of(TextBodyQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """text="Hello" AND (mid="1" OR mid="2" OR mid="3")"""),
                Arguments.of(TextBodyQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf(), columnName = "mid"), """text="Hello""""),
                Arguments.of(TextBodyQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf("1"), columnName = "mid"), """text="Hello" AND (mid="1")"""),
                Arguments.of(TextBodyQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """text="Hello" AND date>=1 AND (mid="1" OR mid="2" OR mid="3")"""),
                Arguments.of(TextBodyQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf(), columnName = "mid"), """text="Hello" AND date>=1"""),
                Arguments.of(TextBodyQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf("1"), columnName = "mid"), """text="Hello" AND date>=1 AND (mid="1")"""),
                Arguments.of(TextBodyQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), ""),
                Arguments.of(TextBodyQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """date>=1"""),
                Arguments.of(TextBodyQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """(mid="1" OR mid="2" OR mid="3")"""),
                Arguments.of(TextBodyQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf(), columnName = "mid"), ""),
                Arguments.of(TextBodyQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf("1"), columnName = "mid"), """(mid="1")"""),
                Arguments.of(TextBodyQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """date>=1 AND (mid="1" OR mid="2" OR mid="3")"""),
                Arguments.of(TextBodyQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf(), columnName = "mid"), """date>=1"""),
                Arguments.of(TextBodyQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf("1"), columnName = "mid"), """date>=1 AND (mid="1")""")
            )
        }
    }

    @ParameterizedTest
    @MethodSource("provideQueries")
    internal fun `queries are built as expected`(
        textBodyQuery: TextBodyQuery?,
        afterDateQuery: AfterDateQuery?,
        messageIdsQuery: MessageIdsQuery?,
        expected: String
    ) {
        // Given
        val queryBuilder = MessageQueryBuilder()
            .addQuery(textBodyQuery)
            .addQuery(afterDateQuery)
            .addQuery(messageIdsQuery)

        // When
        val query = queryBuilder.build()

        // Then
        query.shouldBe(expected)
    }
}