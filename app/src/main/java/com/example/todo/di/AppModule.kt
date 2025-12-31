package com.example.todo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationService
import net.openid.appauth.connectivity.ConnectionBuilder
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideConnectionBuilder(): ConnectionBuilder {
        return ConnectionBuilder { uri ->
            val url = URL(uri.toString())
            url.openConnection() as HttpURLConnection
        }
    }

    @Provides
    @Singleton
    fun provideAuthorizationService(
        @ApplicationContext context: Context,
        connectionBuilder: ConnectionBuilder
    ): AuthorizationService {
        val appAuthConfiguration = AppAuthConfiguration.Builder()
            .setConnectionBuilder(connectionBuilder)
            .build()
        return AuthorizationService(context, appAuthConfiguration)
    }
}
