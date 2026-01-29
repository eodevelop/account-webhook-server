package com.service.accountwebhookserver.controller

import com.service.accountwebhookserver.model.WebhookRequest
import com.service.accountwebhookserver.model.WebhookResponse
import com.service.accountwebhookserver.service.WebhookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/webhooks")
class WebhookController(
    private val webhookService: WebhookService,
) {

    @PostMapping("/account-changes")
    fun receiveWebhook(
        @RequestHeader("X-Signature") signature: String,
        @RequestHeader("X-Event-Id") eventId: String,
        @RequestBody request: WebhookRequest,
    ): ResponseEntity<WebhookResponse> {
        // TODO: 서명 검증 로직 구현
        val response = webhookService.receiveWebhook(eventId, request)
        return ResponseEntity.ok(response)
    }
}
