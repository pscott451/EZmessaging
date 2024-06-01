package com.scott.app.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scott.app.ui.view.reusable.MessageView
import com.scott.app.viewmodel.ShowAllMessagesViewModel
import com.scott.app.viewmodel.ShowAllMessagesViewModel.ConversationState
import com.scott.ezmessaging.model.Message

@Composable
fun ShowAllMessagesScreen(viewModel: ShowAllMessagesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState) {
            is ConversationState.ShowConversationList -> ShowConversations(conversations = (uiState as ConversationState.ShowConversationList).conversations)
            is ConversationState.ShowMessages -> ShowMessages((uiState as ConversationState.ShowMessages).messages)
            ConversationState.BuildingConversations -> CircularProgressIndicator()
        }
    }

}

@Composable
fun ShowConversations(
    conversations: List<ShowAllMessagesViewModel.Conversation>,
    viewModel: ShowAllMessagesViewModel = viewModel()
) {
    LazyColumn {
        items(conversations) { conversation ->
            ConversationItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 20.dp, bottom = 20.dp),
                conversation = conversation,
                onClick = {
                    viewModel.getMessages(conversation.threadId)
                }
            )
        }
    }
}

@Composable
fun ShowMessages(messages: List<Message>) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }
    LazyColumn(state = listState) {
        items(messages) { message ->
            MessageView(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 20.dp, bottom = 20.dp),
                message = message
            )
        }
    }
}

@Composable
fun ConversationItem(
    modifier: Modifier = Modifier,
    conversation: ShowAllMessagesViewModel.Conversation,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable {
            onClick()
        },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        conversation.participants.forEach {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Cyan)
                    .padding(top = 20.dp, bottom = 20.dp),
                text = it,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}