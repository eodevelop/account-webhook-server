package com.service.accountwebhookserver.model

data class WebhookRequest(
    val accountKey: String,
    val eventType: String,
    val data: Map<String, Any>? = null,
)
