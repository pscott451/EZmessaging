package com.scott.ezmessaging.module

import com.scott.ezmessaging.MainCoroutineRule
import com.scott.ezmessaging.UnconfinedCoroutineRule
import com.scott.ezmessaging.manager.ContentManagerImpl
import com.scott.ezmessaging.provider.DispatchProviderImpl
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(UnconfinedCoroutineRule::class)
class MessagingModuleTest {

    @Test
    fun `provideContentManager provides the ContentManagerImpl`() {
        // Given
        val messagingModule = MessagingModule

        // When
        val contentManager = messagingModule.provideContentManager(MainCoroutineRule.dispatcherProvider, mockk(), mockk(), mockk())

        // Then
        contentManager.shouldBeInstanceOf<ContentManagerImpl>()
    }

    @Test
    fun `provideDispatchProvider provides the DispatchProviderImpl`() {
        // Given
        val messagingModule = MessagingModule

        // When
        val dispatcherProvider = messagingModule.provideDispatchProvider()

        // Then
        dispatcherProvider.shouldBeInstanceOf<DispatchProviderImpl>()
    }

}