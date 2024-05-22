package com.scott.ezmessaging.contentresolver

/**
 * Builds queries that can be used for retrieving messages from the database.
 * Usage:
 * Add [Query]s to the builder and [build].
 * MessageQueryBuilder()
 *             .addQuery(TextBodyQuery(text = text, columnName = COLUMN_SMS_BODY))
 *             .addQuery(AfterDateQuery(dateMillis = dateInMillis, columnName = COLUMN_SMS_DATE_RECEIVED))
 *             .addQuery(MessageIdsQuery(ids = msgIds, columnName = COLUMN_MMS_MID))
 *             .build()
 *
 * Example return: "text="Hello" AND date>=1 AND (mid="1" OR mid="2" OR mid="3")"
 */
internal class MessageQueryBuilder {

    private var textBodyQuery: Query.TextBodyQuery? = null
    private var afterDateQuery: Query.AfterDateQuery? = null
    private var messageIdsQuery: Query.MessageIdsQuery? = null

    fun addQuery(query: Query?): MessageQueryBuilder {
        when (query) {
            is Query.TextBodyQuery -> textBodyQuery = query
            is Query.AfterDateQuery -> afterDateQuery = query
            is Query.MessageIdsQuery -> messageIdsQuery = query
            null -> { /* no op */ }
        }
        return this
    }

    fun build(): String {
        var filter = ""
        val textBodyQuery = textBodyQuery
        val afterDateQuery = afterDateQuery
        val messageIdsQuery = messageIdsQuery
        if (textBodyQuery?.text != null) filter += textBodyQuery.toQuery()
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

        data class TextBodyQuery(
            val text: String?,
            override val columnName: String
        ) : Query {
            override fun toQuery() = text?.let { """$columnName="$it"""" } ?: ""
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

