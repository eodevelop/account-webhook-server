package com.service.accountwebhookserver.model

data class CreateAccountRequest(
    val accountKey: String,
    val email: String,
)
