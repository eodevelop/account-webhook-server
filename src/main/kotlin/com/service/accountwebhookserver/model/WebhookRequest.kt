package com.service.accountwebhookserver.model

import com.service.accountwebhookserver.common.EventType

data class WebhookRequest(
    val accountKey: String,
    val eventType: EventType,
    val data: Map<String, Any>? = null,
)
