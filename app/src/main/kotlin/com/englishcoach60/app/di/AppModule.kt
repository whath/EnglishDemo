package com.englishcoach60.app.di

import android.content.Context
import com.englishcoach60.speech.AndroidSpeechController
import com.englishcoach60.speech.AndroidSpeechSynthesizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun speechController(@ApplicationContext context: Context) = AndroidSpeechController(context)
    @Provides @Singleton fun speechSynthesizer(@ApplicationContext context: Context) = AndroidSpeechSynthesizer(context)
}
