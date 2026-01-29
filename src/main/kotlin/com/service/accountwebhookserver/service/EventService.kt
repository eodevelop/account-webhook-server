package com.service.accountwebhookserver.service

import com.service.accountwebhookserver.model.EventResponse
import com.service.accountwebhookserver.repository.WebhookEventRepository
import org.springframework.stereotype.Service

@Service
class EventService(
    private val webhookEventRepository: WebhookEventRepository,
) {

    fun getEvent(eventId: String): EventResponse? {
        return webhookEventRepository.findByEventId(eventId)
    }
}
