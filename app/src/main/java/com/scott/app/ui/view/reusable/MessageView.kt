package com.scott.app.ui.view.reusable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scott.ezmessaging.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageView(
    modifier: Modifier = Modifier,
    message: Message
) {
    val dateFormat = SimpleDateFormat("MMM dd yyyy HH:mm:ss", Locale.getDefault())
    val date = Date(message.dateReceived)
    val formattedDate = dateFormat.format(date)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Cyan)
                .padding(top = 20.dp),
            text = message.senderAddress,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Cyan),
            text = formattedDate,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        when (message) {
            is Message.MmsMessage -> {
                if (message.hasImage) {
                    ShowMessageImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Cyan)
                            .padding(top = 10.dp, bottom = 20.dp),
                        message
                    )
                } else {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Cyan)
                            .padding(top = 10.dp, bottom = 20.dp),
                        text = message.text ?: "",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is Message.SmsMessage -> Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Cyan)
                    .padding(top = 10.dp, bottom = 20.dp),
                text = message.text,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}