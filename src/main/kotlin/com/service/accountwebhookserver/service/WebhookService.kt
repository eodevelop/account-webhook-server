package com.service.accountwebhookserver.service

import com.service.accountwebhookserver.model.WebhookRequest
import com.service.accountwebhookserver.model.WebhookResponse
import com.service.accountwebhookserver.repository.WebhookEventRepository
import org.springframework.stereotype.Service

@Service
class WebhookService(
    private val webhookEventRepository: WebhookEventRepository,
) {

    fun receiveWebhook(eventId: String, request: WebhookRequest): WebhookResponse {
        // TODO: 중복 처리 방지 로직 구현
        // TODO: 처리 로직 구현
        return WebhookResponse("received", "이벤트 수신됨")
    }
}
