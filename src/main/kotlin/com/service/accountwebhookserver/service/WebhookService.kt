package com.service.accountwebhookserver.service

import com.service.accountwebhookserver.common.AccountStatus
import com.service.accountwebhookserver.common.EventStatus
import com.service.accountwebhookserver.common.EventType
import com.service.accountwebhookserver.model.WebhookRequest
import com.service.accountwebhookserver.model.WebhookResponse
import com.service.accountwebhookserver.repository.AccountRepository
import com.service.accountwebhookserver.repository.WebhookEventRepository
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class WebhookService(
    private val webhookEventRepository: WebhookEventRepository,
    private val accountRepository: AccountRepository,
    private val objectMapper: ObjectMapper,
) {

    fun receiveWebhook(eventId: String, request: WebhookRequest): WebhookResponse {
        // 중복 처리 방지: 이미 수신한 이벤트인지 확인
        if (webhookEventRepository.existsByEventId(eventId)) {
            return WebhookResponse("duplicated", "이미 수신된 이벤트입니다")
        }

        // 이벤트 저장
        val payload = objectMapper.writeValueAsString(request)
        webhookEventRepository.save(eventId, request.eventType.name, payload)

        // 이벤트 처리
        return processEvent(eventId, request)
    }

    private fun processEvent(eventId: String, request: WebhookRequest): WebhookResponse {
        webhookEventRepository.updateStatus(eventId, EventStatus.PROCESSING)

        return try {
            when (request.eventType) {
                EventType.EMAIL_FORWARDING_CHANGED -> handleEmailForwardingChanged(request)
                EventType.ACCOUNT_DELETED -> handleAccountDeleted(request)
                EventType.APPLE_ACCOUNT_DELETED -> handleAppleAccountDeleted(request)
            }
            webhookEventRepository.updateStatus(eventId, EventStatus.DONE)
            WebhookResponse("processed", "이벤트 처리 완료")
        } catch (e: Exception) {
            webhookEventRepository.updateStatus(eventId, EventStatus.FAILED, e.message)
            WebhookResponse("failed", "이벤트 처리 실패: ${e.message}")
        }
    }

    private fun handleEmailForwardingChanged(request: WebhookRequest) {
        val email = request.data?.get("email") as? String
            ?: throw IllegalArgumentException("email이 필요합니다")
        accountRepository.updateEmail(request.accountKey, email)
            ?: throw IllegalArgumentException("계정을 찾을 수 없습니다: ${request.accountKey}")
    }

    private fun handleAccountDeleted(request: WebhookRequest) {
        accountRepository.updateStatus(request.accountKey, AccountStatus.DELETED)
            ?: throw IllegalArgumentException("계정을 찾을 수 없습니다: ${request.accountKey}")
    }

    private fun handleAppleAccountDeleted(request: WebhookRequest) {
        accountRepository.updateStatus(request.accountKey, AccountStatus.APPLE_DELETED)
            ?: throw IllegalArgumentException("계정을 찾을 수 없습니다: ${request.accountKey}")
    }
}
