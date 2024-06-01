package com.scott.app.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.scott.app.ui.theme.TestAppTheme
import com.scott.app.ui.view.ShowAllMessagesScreen
import com.scott.app.viewmodel.ShowAllMessagesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShowAllMessagesActivity: ComponentActivity() {

    private val showAllMessagesViewModel by viewModels<ShowAllMessagesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestAppTheme {
                ShowAllMessagesScreen()
            }
        }
        onBackPressedDispatcher.addCallback {
            if (showAllMessagesViewModel.uiState.value is ShowAllMessagesViewModel.ConversationState.ShowConversationList) {
                finish()
            } else {
                showAllMessagesViewModel.showConversationList()
            }
        }
    }
}