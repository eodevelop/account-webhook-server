package com.service.accountwebhookserver.repository

import com.service.accountwebhookserver.common.EventStatus
import com.service.accountwebhookserver.entity.WebhookEvents
import com.service.accountwebhookserver.model.EventResponse
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class WebhookEventRepository {

    fun save(eventId: String, eventType: String, payload: String): EventResponse = transaction {
        WebhookEvents.insert {
            it[WebhookEvents.eventId] = eventId
            it[WebhookEvents.eventType] = eventType
            it[WebhookEvents.payload] = payload
            it[status] = EventStatus.RECEIVED.name
        }

        findByEventIdInternal(eventId)!!
    }

    fun updateStatus(
        eventId: String,
        status: EventStatus,
        errorMessage: String? = null,
    ): EventResponse? = transaction {
        val updated = WebhookEvents.update({ WebhookEvents.eventId eq eventId }) {
            it[WebhookEvents.status] = status.name
            it[WebhookEvents.errorMessage] = errorMessage
            if (status == EventStatus.DONE || status == EventStatus.FAILED) {
                it[processedAt] = Instant.now()
            }
        }
        if (updated > 0) findByEventIdInternal(eventId) else null
    }

    fun findByEventId(eventId: String): EventResponse? = transaction {
        findByEventIdInternal(eventId)
    }

    fun existsByEventId(eventId: String): Boolean = transaction {
        WebhookEvents.select(WebhookEvents.id)
            .where { WebhookEvents.eventId eq eventId }
            .count() > 0
    }

    private fun findByEventIdInternal(eventId: String): EventResponse? {
        return WebhookEvents.select(WebhookEvents.columns)
            .where { WebhookEvents.eventId eq eventId }
            .map { it.toEventResponse() }
            .singleOrNull()
    }

    private fun ResultRow.toEventResponse() = EventResponse(
        eventId = this[WebhookEvents.eventId],
        eventType = this[WebhookEvents.eventType],
        status = this[WebhookEvents.status],
        createdAt = this[WebhookEvents.createdAt],
        processedAt = this[WebhookEvents.processedAt],
    )
}
