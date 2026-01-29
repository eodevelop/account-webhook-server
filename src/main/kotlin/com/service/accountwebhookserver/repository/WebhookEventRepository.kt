package com.service.accountwebhookserver.repository

import com.service.accountwebhookserver.entity.WebhookEvents
import com.service.accountwebhookserver.model.EventResponse
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

@Repository
class WebhookEventRepository {

    fun findByEventId(eventId: String): EventResponse? = transaction {
        WebhookEvents.select(WebhookEvents.columns)
            .where { WebhookEvents.eventId eq eventId }
            .map { it.toEventResponse() }
            .singleOrNull()
    }

    fun existsByEventId(eventId: String): Boolean = transaction {
        WebhookEvents.select(WebhookEvents.id)
            .where { WebhookEvents.eventId eq eventId }
            .count() > 0
    }

    private fun ResultRow.toEventResponse() = EventResponse(
        eventId = this[WebhookEvents.eventId],
        eventType = this[WebhookEvents.eventType],
        status = this[WebhookEvents.status],
        createdAt = this[WebhookEvents.createdAt],
        processedAt = this[WebhookEvents.processedAt],
    )
}
