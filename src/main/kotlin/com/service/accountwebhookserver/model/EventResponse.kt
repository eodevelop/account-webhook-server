package com.service.accountwebhookserver.model

import java.time.Instant

data class EventResponse(
    val eventId: String,
    val eventType: String,
    val status: String,
    val createdAt: Instant,
    val processedAt: Instant? = null,
)
