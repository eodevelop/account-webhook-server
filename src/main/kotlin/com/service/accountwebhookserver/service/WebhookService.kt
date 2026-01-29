package com.service.accountwebhookserver.service

import com.service.accountwebhookserver.model.WebhookRequest
import com.service.accountwebhookserver.model.WebhookResponse
import com.service.accountwebhookserver.repository.WebhookEventRepository
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class WebhookService(
    private val webhookEventRepository: WebhookEventRepository,
    private val objectMapper: ObjectMapper,
) {

    fun receiveWebhook(eventId: String, request: WebhookRequest): WebhookResponse {
        // 중복 처리 방지: 이미 수신한 이벤트인지 확인
        if (webhookEventRepository.existsByEventId(eventId)) {
            return WebhookResponse("duplicated", "이미 수신된 이벤트입니다")
        }

        // 이벤트 저장
        val payload = objectMapper.writeValueAsString(request)
        webhookEventRepository.save(eventId, request.eventType, payload)

        // TODO: 이벤트 처리 로직 구현 (6단계)
        return WebhookResponse("received", "이벤트 수신됨")
    }
}
