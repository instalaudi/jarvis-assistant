package com.jarvis.assistant.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jarvis.assistant.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jarvis.assistant.data.api.OpenAIApi
import com.jarvis.assistant.data.local.JarvisDatabase
import com.jarvis.assistant.data.local.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val OPENAI_BASE_URL = "https://api.openai.com/"
    private const val ENCRYPTED_PREFS_NAME = "jarvis_secure_prefs"

    @Provides
    @Singleton
    @Suppress("DEPRECATION")
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // BODY en debug para diagnóstico; NONE en release para no filtrar en logcat
        // el header Authorization (API keys) ni el contenido de los chats.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OPENAI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAIApi(retrofit: Retrofit): OpenAIApi {
        return retrofit.create(OpenAIApi::class.java)
    }

    // NOTA: Jetpack Security Crypto (EncryptedSharedPreferences) fue deprecada por Google
    // en security-crypto 1.1.0-alpha07 (abril 2025) y nunca alcanzó una API estable de
    // reemplazo. 1.1.0-alpha06 es la última versión cuya API no está marcada deprecada.
    // Migración recomendada a futuro: Android Keystore (AES/GCM) + DataStore.
    @Provides
    @Singleton
    @Named("encrypted")
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideJarvisDatabase(
        @ApplicationContext context: Context
    ): JarvisDatabase {
        return JarvisDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: JarvisDatabase): MessageDao {
        return database.messageDao()
    }
}

