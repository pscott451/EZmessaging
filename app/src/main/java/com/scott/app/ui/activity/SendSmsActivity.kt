package com.scott.app.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.scott.app.ui.theme.TestAppTheme
import com.scott.app.ui.view.SendSmsMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SendSmsActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestAppTheme {
                SendSmsMessage()
            }
        }
    }
}