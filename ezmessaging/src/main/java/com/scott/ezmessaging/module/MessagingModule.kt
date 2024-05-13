package com.scott.ezmessaging.module

import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.manager.ContentManagerImpl
import com.scott.ezmessaging.manager.DeviceManager
import com.scott.ezmessaging.manager.MmsManager
import com.scott.ezmessaging.manager.SmsManager
import com.scott.ezmessaging.provider.DispatchProviderImpl
import com.scott.ezmessaging.provider.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MessagingModule {

    @Provides
    @Singleton
    internal fun provideContentManager(
        dispatcherProvider: DispatcherProvider,
        smsManager: SmsManager,
        mmsManager: MmsManager,
        deviceManager: DeviceManager
    ): ContentManager = ContentManagerImpl(
        smsManager,
        mmsManager,
        deviceManager,
        dispatcherProvider
    )

    @Provides
    @Singleton
    internal fun provideDispatchProvider(): DispatcherProvider = DispatchProviderImpl()
}
