package com.scott.app.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scott.app.R
import com.scott.app.ui.view.reusable.InputField
import com.scott.app.ui.view.reusable.ReceivedMessages
import com.scott.app.viewmodel.SmsViewModel

@Composable
fun SendSmsMessage(viewModel: SmsViewModel = viewModel()) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current

        val smsMessages by viewModel.receivedMessages.collectAsState()
        val sendingInProgress by viewModel.sendingInProgress.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var recipient by remember { mutableStateOf("") }
            var message by remember { mutableStateOf("") }
            InputField(
                modifier = Modifier.padding(bottom = 10.dp),
                label = R.string.sendSms_recipientslabel,
                initialValue = recipient,
                onValueChange = { recipient = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            InputField(
                label = R.string.sendSms_messagelabel,
                initialValue = message,
                onValueChange = { message = it }
            )
            if (sendingInProgress) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 20.dp))
            } else {
                Button(
                    modifier = Modifier.padding(top = 20.dp),
                    onClick = {
                        viewModel.sendMessage(recipient, message)
                        keyboardController?.hide()
                    }) {
                    Text(text = stringResource(id = R.string.sendSms_sendmessagebutton))
                }
            }
        }

        ReceivedMessages(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            messages = smsMessages
        )
    }
}