package com.example.ainotes.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.ainotes.chatGPT.AuthInterceptor
import com.example.ainotes.chatGPT.ChatGPTApiService
import com.example.ainotes.utils.BaseUrlInterceptor
import com.example.ainotes.utils.BaseUrlManager
import com.example.ainotes.utils.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


/** Модуль для предоставления Retrofit, API и репозитория */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideBaseUrlManager(@ApplicationContext ctx: Context) =
        BaseUrlManager(ctx)

    @Provides @Singleton
    fun provideOkHttpClient(
        baseUrlManager: BaseUrlManager
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.SECONDS)
            .readTimeout(1000, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor())
            .addInterceptor(BaseUrlInterceptor(baseUrlManager))
            .build()

    @Provides @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideChatGPTApiService(retrofit: Retrofit): ChatGPTApiService =
        retrofit.create(ChatGPTApiService::class.java)

    @Provides @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore
}