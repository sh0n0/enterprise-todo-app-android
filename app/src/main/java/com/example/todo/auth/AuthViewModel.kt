package com.example.todo.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _authStateMessage = MutableStateFlow("Not logged in")
        val authStateMessage: StateFlow<String> = _authStateMessage.asStateFlow()

        private val _authRequest = Channel<AuthorizationRequest>(Channel.BUFFERED)
        val authRequest = _authRequest.receiveAsFlow()

        fun startAuth() {
            viewModelScope.launch {
                try {
                    val request = authRepository.getAuthRequest()
                    _authRequest.send(request)
                } catch (ex: Exception) {
                    _authStateMessage.value = "Failed to retrieve discovery document: ${ex.message}"
                    Log.e("MainViewModel", "Discovery failed", ex)
                }
            }
        }

        fun handleAuthResult(
            response: AuthorizationResponse?,
            ex: AuthorizationException?,
        ) {
            if (response != null) {
                _authStateMessage.value = "Authorization code received. Exchanging for token..."
                viewModelScope.launch {
                    try {
                        val tokenResponse = authRepository.performTokenRequest(response)
                        _authStateMessage.value =
                            "Login successful!\nAccess Token: ${tokenResponse.accessToken?.take(20)}..."
                        Log.d("MainViewModel", "AccessToken: ${tokenResponse.accessToken}")
                        Log.d("MainViewModel", "RefreshToken: ${tokenResponse.refreshToken}")
                    } catch (e: Exception) {
                        _authStateMessage.value = "Token exchange failed: ${e.message}"
                        Log.e("MainViewModel", "Token exchange failed", e)
                    }
                }
            } else {
                _authStateMessage.value = "Authorization failed: ${ex?.message}"
            }
        }
    }