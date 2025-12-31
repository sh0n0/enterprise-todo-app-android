package com.example.todo.auth

import android.net.Uri

data class AuthConfig(
    val issuerUri: Uri,
    val clientId: String,
    val redirectUri: Uri,
)