package com.scott.app.ui.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scott.app.R
import com.scott.app.ui.view.reusable.InputField
import com.scott.app.ui.view.reusable.ReceivedMessages
import com.scott.app.viewmodel.MmsViewModel
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.model.MessageData

@Composable
fun SendMmsMessage(viewModel: MmsViewModel = viewModel()) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val sendingInProgress by viewModel.sendingInProgress.collectAsState()
    val mmsMessages by viewModel.receivedMessages.collectAsState()
    var recipients by remember { mutableStateOf("") }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        it?.let { uri ->
            viewModel.sendMessage(MessageData.ContentUri(uri), recipients.split(",").toTypedArray())
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var message by remember { mutableStateOf("") }
            Text(
                textAlign = TextAlign.Center, text = stringResource(id = R.string.sendMms_recipientsheader)
            )
            InputField(
                modifier = Modifier.padding(bottom = 10.dp),
                label = R.string.sendMms_recipientslabel,
                initialValue = recipients,
                onValueChange = { recipients = it }
            )
            InputField(
                label = R.string.sendMms_messagelabel,
                initialValue = message,
                onValueChange = { message = it }
            )
            if (sendingInProgress) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 20.dp))
            } else {
                Button(modifier = Modifier.padding(top = 20.dp), onClick = {
                    viewModel.sendMessage(
                        MessageData.Text(message), recipients.split(",").toTypedArray()
                    )
                    keyboardController?.hide()
                }) {
                    Text(text = stringResource(id = R.string.sendMms_sendmessagebutton))
                }
                Button(modifier = Modifier.padding(top = 20.dp), onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.SingleMimeType(ContentManager.SupportedMessageTypes.CONTENT_TYPE_JPEG)))
                    keyboardController?.hide()
                }) {
                    Text(text = stringResource(id = R.string.sendMms_sendimagebutton))
                }
            }
        }
        ReceivedMessages(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            messages = mmsMessages
        )
    }
}