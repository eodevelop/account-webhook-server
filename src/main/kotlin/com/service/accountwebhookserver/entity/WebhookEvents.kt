package com.service.accountwebhookserver.entity

import com.service.accountwebhookserver.common.EventStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object WebhookEvents : Table("webhook_event") {
    val id = long("id").autoIncrement()
    val eventId = varchar("event_id", 255).uniqueIndex()
    val eventType = varchar("event_type", 100)
    val payload = text("payload")
    val status = varchar("status", 50).default(EventStatus.RECEIVED.name)
    val errorMessage = text("error_message").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    val processedAt = timestamp("processed_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
