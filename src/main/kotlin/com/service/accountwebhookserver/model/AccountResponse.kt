package com.service.accountwebhookserver.model

data class AccountResponse(
    val accountKey: String,
    val email: String,
    val status: String,
)
