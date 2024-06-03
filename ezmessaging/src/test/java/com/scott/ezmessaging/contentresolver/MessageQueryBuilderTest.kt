package com.scott.ezmessaging.contentresolver

import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.AfterDateQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.ContainsTextQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.ExactTextQuery
import com.scott.ezmessaging.contentresolver.MessageQueryBuilder.Query.MessageIdsQuery
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
                /* 0 0 0 0 */Arguments.of(ExactTextQuery(text = null, columnName = "text"), ContainsTextQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), ""),
                /* 0 0 0 1 */Arguments.of(ExactTextQuery(text = null, columnName = "text"), ContainsTextQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """(mid="1" OR mid="2" OR mid="3")"""),
                /* 0 0 1 0 */Arguments.of(ExactTextQuery(text = null, columnName = "text"), ContainsTextQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """date>=1"""),
                /* 0 0 1 1 */Arguments.of(ExactTextQuery(text = null, columnName = "text"), ContainsTextQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """date>=1 AND (mid="1" OR mid="2" OR mid="3")"""),
                /* 0 1 0 0 */Arguments.of(ExactTextQuery(text = null, columnName = "text"), ContainsTextQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """text LIKE "%Hello%""""),
                /* 0 1 0 1 */Arguments.of(ExactTextQuery(text = null, columnName = "text"), ContainsTextQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """text LIKE "%Hello%" AND (mid="1" OR mid="2" OR mid="3")"""),
                /* 0 1 1 0 */Arguments.of(ExactTextQuery(text = null, columnName = "text"), ContainsTextQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """text LIKE "%Hello%" AND date>=1"""),
                /* 0 1 1 1 */Arguments.of(ExactTextQuery(text = null, columnName = "text"), ContainsTextQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """text LIKE "%Hello%" AND date>=1 AND (mid="1" OR mid="2" OR mid="3")"""),
                /* 1 0 0 0 */Arguments.of(ExactTextQuery(text = "Hello", columnName = "text"), ContainsTextQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """text="Hello""""),
                /* 1 0 0 1 */Arguments.of(ExactTextQuery(text = "Hello", columnName = "text"), ContainsTextQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """text="Hello" AND (mid="1" OR mid="2" OR mid="3")"""),
                /* 1 0 1 0 */Arguments.of(ExactTextQuery(text = "Hello", columnName = "text"), ContainsTextQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """text="Hello" AND date>=1"""),
                /* 1 0 1 1 */Arguments.of(ExactTextQuery(text = "Hello", columnName = "text"), ContainsTextQuery(text = null, columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """text="Hello" AND date>=1 AND (mid="1" OR mid="2" OR mid="3")"""),
                /* 1 1 0 0 */Arguments.of(ExactTextQuery(text = "Hello", columnName = "text"), ContainsTextQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """text="Hello" AND text LIKE "%Hello%""""),
                /* 1 1 0 1 */Arguments.of(ExactTextQuery(text = "Hello", columnName = "text"), ContainsTextQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = null, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """text="Hello" AND text LIKE "%Hello%" AND (mid="1" OR mid="2" OR mid="3")"""),
                /* 1 1 1 0 */Arguments.of(ExactTextQuery(text = "Hello", columnName = "text"), ContainsTextQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = null, columnName = "mid"), """text="Hello" AND text LIKE "%Hello%" AND date>=1"""),
                /* 1 1 1 1 */Arguments.of(ExactTextQuery(text = "Hello", columnName = "text"), ContainsTextQuery(text = "Hello", columnName = "text"), AfterDateQuery(dateMillis = 1, columnName = "date"), MessageIdsQuery(ids = setOf("1", "2", "3"), columnName = "mid"), """text="Hello" AND text LIKE "%Hello%" AND date>=1 AND (mid="1" OR mid="2" OR mid="3")"""),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("provideQueries")
    internal fun `queries are built as expected`(
        exactTextQuery: ExactTextQuery?,
        containsTextQuery: ContainsTextQuery?,
        afterDateQuery: AfterDateQuery?,
        messageIdsQuery: MessageIdsQuery?,
        expected: String
    ) {
        // Given
        val queryBuilder = MessageQueryBuilder()
            .addQuery(exactTextQuery)
            .addQuery(containsTextQuery)
            .addQuery(afterDateQuery)
            .addQuery(messageIdsQuery)

        // When
        val query = queryBuilder.build()

        // Then
        query.shouldBe(expected)
    }
}