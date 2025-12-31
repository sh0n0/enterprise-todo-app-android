package com.example.todo.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import net.openid.appauth.connectivity.ConnectionBuilder
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository
    @Inject
    constructor(
        private val authService: AuthorizationService,
        private val connectionBuilder: ConnectionBuilder,
        private val authConfig: AuthConfig,
    ) {
        suspend fun getAuthRequest(): AuthorizationRequest =
            suspendCancellableCoroutine { continuation ->
                AuthorizationServiceConfiguration.fetchFromIssuer(
                    authConfig.issuerUri,
                    { config, ex ->
                        if (ex != null) {
                            continuation.resumeWithException(ex)
                            return@fetchFromIssuer
                        }
                        if (config != null) {
                            val authRequest =
                                AuthorizationRequest
                                    .Builder(
                                        config,
                                        authConfig.clientId,
                                        ResponseTypeValues.CODE,
                                        authConfig.redirectUri,
                                    ).build()
                            continuation.resume(authRequest)
                        } else {
                            continuation.resumeWithException(IllegalStateException("Configuration was null"))
                        }
                    },
                    connectionBuilder,
                )
            }

        suspend fun performTokenRequest(response: AuthorizationResponse): TokenResponse =
            suspendCancellableCoroutine { continuation ->
                authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, exception ->
                    if (tokenResponse != null) {
                        continuation.resume(tokenResponse)
                    } else {
                        val ex = exception ?: AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR
                        continuation.resumeWithException(ex)
                    }
                }
            }
    }