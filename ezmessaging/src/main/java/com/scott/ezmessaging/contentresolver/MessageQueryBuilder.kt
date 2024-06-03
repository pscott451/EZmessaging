package com.scott.ezmessaging.contentresolver

/**
 * Builds queries that can be used for retrieving messages from the database.
 * Usage:
 * Add [Query]s to the builder and [build].
 * MessageQueryBuilder()
 *             .addQuery(ExactTextQuery(text = text, columnName = COLUMN_SMS_BODY))
 *             .addQuery(ContainsTextQuery(text = text, columnName = COLUMN_SMS_BODY))
 *             .addQuery(AfterDateQuery(dateMillis = dateInMillis, columnName = COLUMN_SMS_DATE_RECEIVED))
 *             .addQuery(MessageIdsQuery(ids = msgIds, columnName = COLUMN_MMS_MID))
 *             .build()
 *
 * Example return: "text="Hello" AND "text LIKE "%Hello%" AND date>=1 AND (mid="1" OR mid="2" OR mid="3")"
 */
internal class MessageQueryBuilder {

    private var exactTextQuery: Query.ExactTextQuery? = null
    private var containsTextQuery: Query.ContainsTextQuery? = null
    private var afterDateQuery: Query.AfterDateQuery? = null
    private var messageIdsQuery: Query.MessageIdsQuery? = null

    fun addQuery(query: Query?): MessageQueryBuilder {
        when (query) {
            is Query.ExactTextQuery -> exactTextQuery = query
            is Query.ContainsTextQuery -> containsTextQuery = query
            is Query.AfterDateQuery -> afterDateQuery = query
            is Query.MessageIdsQuery -> messageIdsQuery = query
            null -> { /* no op */ }
        }
        return this
    }

    fun build(): String {
        var filter = ""
        val exactTextQuery = exactTextQuery
        val containsTextQuery = containsTextQuery
        val afterDateQuery = afterDateQuery
        val messageIdsQuery = messageIdsQuery
        if (exactTextQuery?.text != null) filter += exactTextQuery.toQuery()
        if (containsTextQuery?.text != null) {
            if (filter.isNotEmpty()) filter += " AND "
            filter += containsTextQuery.toQuery()
        }
        if (afterDateQuery?.dateMillis != null) {
            if (filter.isNotEmpty()) filter += " AND "
            filter += afterDateQuery.toQuery()
        }
        if (messageIdsQuery?.ids?.isNotEmpty() == true) {
            if (filter.isNotEmpty()) filter += " AND "
            filter += messageIdsQuery.toQuery()
        }
        return filter
    }

    sealed interface Query {
        val columnName: String
        fun toQuery(): String

        data class ExactTextQuery(
            val text: String?,
            override val columnName: String
        ) : Query {
            override fun toQuery() = text?.let { """$columnName="$it"""" } ?: ""
        }

        data class ContainsTextQuery(
            val text: String?,
            override val columnName: String
        ) : Query {
            override fun toQuery() = text?.let { """$columnName LIKE "%$it%"""" } ?: ""
        }

        data class AfterDateQuery(
            val dateMillis: Long?,
            override val columnName: String
        ) : Query {
            override fun toQuery() = dateMillis?.let { "$columnName>=$dateMillis" } ?: ""
        }

        data class MessageIdsQuery(
            val ids: Set<String>?,
            override val columnName: String
        ) : Query {
            override fun toQuery(): String {
                if (ids.isNullOrEmpty()) return ""
                var query = "("
                ids.forEachIndexed { i, messageId ->
                    query += when {
                        ids.size == 1 -> """$columnName="$messageId")"""
                        i == 0 -> """$columnName="$messageId""""
                        i == ids.size - 1 -> """ OR $columnName="$messageId")"""
                        else -> """ OR $columnName="$messageId""""
                    }
                }
                return query
            }
        }
    }
}

