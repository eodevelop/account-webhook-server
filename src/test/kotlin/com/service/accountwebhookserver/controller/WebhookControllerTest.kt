package com.service.accountwebhookserver.controller

import com.ninjasquad.springmockk.MockkBean
import com.service.accountwebhookserver.common.EventType
import com.service.accountwebhookserver.model.WebhookRequest
import com.service.accountwebhookserver.model.WebhookResponse
import com.service.accountwebhookserver.service.WebhookService
import com.service.accountwebhookserver.util.SignatureValidator
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(WebhookController::class)
class WebhookControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var webhookService: WebhookService

    @MockkBean
    private lateinit var signatureValidator: SignatureValidator

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        every { signatureValidator.verify(any(), any()) } returns true
    }

    @Test
    fun `웹훅 수신 성공 시 200 OK 반환`() {
        // Given
        val eventId = "evt-12345678"
        val request = WebhookRequest(
            accountKey = "acc-12345678",
            eventType = EventType.ACCOUNT_DELETED,
            data = null,
        )
        val response = WebhookResponse(
            status = "processed",
            message = "이벤트 처리 완료",
        )
        every { webhookService.receiveWebhook(eventId, any()) } returns response

        // When & Then
        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", eventId)
            header("X-Signature", "test-signature")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("processed") }
            jsonPath("$.message") { value("이벤트 처리 완료") }
        }
    }

    @Test
    fun `중복 이벤트 수신 시 duplicated 응답`() {
        // Given
        val eventId = "evt-duplicate"
        val request = WebhookRequest(
            accountKey = "acc-12345678",
            eventType = EventType.ACCOUNT_DELETED,
            data = null,
        )
        val response = WebhookResponse(
            status = "duplicated",
            message = "이미 수신된 이벤트입니다",
        )
        every { webhookService.receiveWebhook(eventId, any()) } returns response

        // When & Then
        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", eventId)
            header("X-Signature", "test-signature")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("duplicated") }
        }
    }

    @Test
    fun `EMAIL_FORWARDING_CHANGED 이벤트 처리`() {
        // Given
        val eventId = "evt-email-change"
        val request = WebhookRequest(
            accountKey = "acc-12345678",
            eventType = EventType.EMAIL_FORWARDING_CHANGED,
            data = mapOf("email" to "new@example.com"),
        )
        val response = WebhookResponse(
            status = "processed",
            message = "이메일 변경 완료",
        )
        every { webhookService.receiveWebhook(eventId, any()) } returns response

        // When & Then
        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", eventId)
            header("X-Signature", "test-signature")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("processed") }
        }
    }

    @Test
    fun `X-Signature 헤더 누락 시 401 Unauthorized`() {
        // Given
        val request = WebhookRequest(
            accountKey = "acc-12345678",
            eventType = EventType.ACCOUNT_DELETED,
            data = null,
        )

        // When & Then
        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", "evt-12345678")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `서명 검증 실패 시 401 Unauthorized`() {
        // Given
        every { signatureValidator.verify(any(), any()) } returns false
        val request = WebhookRequest(
            accountKey = "acc-12345678",
            eventType = EventType.ACCOUNT_DELETED,
            data = null,
        )

        // When & Then
        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", "evt-12345678")
            header("X-Signature", "invalid-signature")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
