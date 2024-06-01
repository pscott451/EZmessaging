package com.scott.app.ui.view.reusable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scott.app.R
import com.scott.ezmessaging.model.Message

@Composable
fun ReceivedMessages(
    modifier: Modifier = Modifier,
    messages: List<Message>
) {
    Column(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.scrollToItem(messages.lastIndex)
            }
        }
        Text(
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.all_receivedmessagesheader)
        )
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
}