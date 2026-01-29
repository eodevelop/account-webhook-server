package com.service.accountwebhookserver.support

import com.service.accountwebhookserver.common.AccountStatus
import com.service.accountwebhookserver.common.EventStatus
import com.service.accountwebhookserver.common.EventType
import com.service.accountwebhookserver.model.AccountResponse
import com.service.accountwebhookserver.model.CreateAccountRequest
import com.service.accountwebhookserver.model.EventResponse
import com.service.accountwebhookserver.model.WebhookRequest
import com.service.accountwebhookserver.model.WebhookResponse
import net.datafaker.Faker
import java.time.Instant
import java.util.Locale
import kotlin.collections.random

object DummyModelFactory {
    private val faker = Faker(Locale.KOREA)

    fun generateWebhookRequest(
        accountKey: String = generateAccountKey(),
        eventType: EventType = EventType.entries.random(),
        data: Map<String, Any>? = null,
    ) = WebhookRequest(
        accountKey = accountKey,
        eventType = eventType,
        data = data,
    )

    fun generateAccountResponse(
        accountKey: String = generateAccountKey(),
        email: String = generateEmail(),
        status: String = AccountStatus.ACTIVE.name,
    ) = AccountResponse(
        accountKey = accountKey,
        email = email,
        status = status,
    )

    fun generateCreateAccountRequest(
        accountKey: String = generateAccountKey(),
        email: String = generateEmail(),
    ) = CreateAccountRequest(
        accountKey = accountKey,
        email = email,
    )

    fun generateEventResponse(
        eventId: String = generateEventId(),
        eventType: String = EventType.entries.random().name,
        status: String = EventStatus.entries.random().name,
        createdAt: Instant = Instant.now(),
        processedAt: Instant? = null,
    ) = EventResponse(
        eventId = eventId,
        eventType = eventType,
        status = status,
        createdAt = createdAt,
        processedAt = processedAt,
    )

    fun generateWebhookResponse(
        status: String = "processed",
        message: String = faker.lorem().sentence(),
    ) = WebhookResponse(
        status = status,
        message = message,
    )

    fun generateAccountKey(): String = "acc-${faker.internet().uuid().take(8)}"

    fun generateEventId(): String = "evt-${faker.internet().uuid().take(8)}"

    fun generateEmail(): String = faker.internet().emailAddress()
}
