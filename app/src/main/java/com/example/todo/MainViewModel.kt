package com.example.todo

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.connectivity.ConnectionBuilder
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authService: AuthorizationService,
        private val connectionBuilder: ConnectionBuilder,
    ) : ViewModel() {
        private val _authStateMessage = MutableStateFlow("Not logged in")
        val authStateMessage: StateFlow<String> = _authStateMessage.asStateFlow()

        private val _authRequest = Channel<AuthorizationRequest>(Channel.BUFFERED)
        val authRequest = _authRequest.receiveAsFlow()

        // Configuration
        private val issuerUri = Uri.parse("http://10.0.2.2:18080/realms/enterprise-todo-app")
        private val clientId = "enterprise-todo-app-mobile"
        private val redirectUri = Uri.parse("com.example.todo:/oauth2redirect")

        fun startAuth() {
            AuthorizationServiceConfiguration.fetchFromIssuer(
                issuerUri,
                { config, ex ->
                    if (ex != null) {
                        _authStateMessage.value = "Failed to retrieve discovery document: ${ex.message}"
                        Log.e("MainViewModel", "Discovery failed", ex)
                        return@fetchFromIssuer
                    }

                    if (config != null) {
                        val authRequest =
                            AuthorizationRequest
                                .Builder(
                                    config,
                                    clientId,
                                    ResponseTypeValues.CODE,
                                    redirectUri,
                                ).build()

                        _authRequest.trySend(authRequest)
                    }
                },
                connectionBuilder,
            )
        }

        fun handleAuthResult(
            response: AuthorizationResponse?,
            ex: AuthorizationException?,
        ) {
            if (response != null) {
                _authStateMessage.value = "Authorization code received. Exchanging for token..."
                authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, exception ->
                    if (tokenResponse != null) {
                        _authStateMessage.value =
                            "Login successful!\nAccess Token: ${tokenResponse.accessToken?.take(20)}..."
                        Log.d("MainViewModel", "AccessToken: ${tokenResponse.accessToken}")
                        Log.d("MainViewModel", "RefreshToken: ${tokenResponse.refreshToken}")
                    } else {
                        _authStateMessage.value = "Token exchange failed: ${exception?.message}"
                    }
                }
            } else {
                _authStateMessage.value = "Authorization failed: ${ex?.message}"
            }
        }
    }