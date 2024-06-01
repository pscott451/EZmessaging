package com.scott.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowAllMessagesViewModel @Inject constructor(
    private val contentManager: ContentManager
): ViewModel() {

    init {
        buildConversations()
    }

    private val _uiState = MutableStateFlow<ConversationState>(ConversationState.BuildingConversations)
    val uiState = _uiState.asStateFlow()

    private val threadIdToConversationMap = mutableMapOf<String, Conversation>()

    private var sortedConversations: List<Conversation> = emptyList()

    private fun buildConversations() {
        viewModelScope.launch {
            val threadIdToMessagesMap = mutableMapOf<String, ArrayList<Message>>()
            contentManager.getAllMessages().forEach {  message ->
                threadIdToMessagesMap[message.threadId]?.add(message) ?: run {
                    threadIdToMessagesMap[message.threadId] = arrayListOf(message)
                }
            }
            threadIdToMessagesMap.forEach { ( threadId , value) ->
                if (value.isNotEmpty()) {
                    val sortedMessages = value.sortedBy { it.dateReceived }
                    threadIdToConversationMap[threadId] = Conversation(
                        threadId = threadId,
                        participants = value.first().participants.toList(),
                        messages = sortedMessages,
                        mostRecentMessage = sortedMessages.last().dateReceived
                    )
                }
            }
            sortedConversations = threadIdToConversationMap.values.sortedByDescending { it.mostRecentMessage }
            _uiState.value = ConversationState.ShowConversationList(sortedConversations)
        }
    }

    fun showConversationList() {
        _uiState.value = ConversationState.ShowConversationList(sortedConversations)
    }

    fun getMessages(threadId: String) {
        _uiState.value = ConversationState.ShowMessages(
            threadIdToConversationMap[threadId]?.messages ?: emptyList()
        )
    }

    data class Conversation(
        val threadId: String,
        val participants: List<String>,
        val messages: List<Message>,
        val mostRecentMessage: Long
    )

    sealed interface ConversationState {
        data object BuildingConversations: ConversationState
        data class ShowConversationList(val conversations: List<Conversation>): ConversationState
        data class ShowMessages(val messages: List<Message>): ConversationState
    }
}