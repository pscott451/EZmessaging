package com.scott.app.ui.view

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.scott.app.ui.activity.SendMmsActivity
import com.scott.app.ui.activity.SendSmsActivity
import com.scott.app.ui.activity.ShowAllMessagesActivity

@Composable
fun MainActivityScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MessageSelectButtons()
    }
}

@Composable
fun MessageSelectButtons() {
    val context = LocalContext.current
    Button(modifier = Modifier
        .padding(bottom = 24.dp)
        .defaultMinSize(200.dp),
        onClick = { context.startActivity(Intent(context, SendSmsActivity::class.java)) }) {
        Text(text = "Send an SMS Message")
    }
    Button(modifier = Modifier
        .padding(bottom = 24.dp)
        .defaultMinSize(200.dp),
        onClick = { context.startActivity(Intent(context, SendMmsActivity::class.java)) }) {
        Text(text = "Send an MMS Message")
    }
    Button(modifier = Modifier
        .padding(bottom = 24.dp)
        .defaultMinSize(200.dp),
        onClick = { context.startActivity(Intent(context, ShowAllMessagesActivity::class.java)) }) {
        Text(text = "Show all Messages")
    }
}

@Preview
@Composable
fun Preview() {
    MainActivityScreen()
}