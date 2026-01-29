package com.service.accountwebhookserver.service

import com.service.accountwebhookserver.common.AccountStatus
import com.service.accountwebhookserver.common.EventStatus
import com.service.accountwebhookserver.common.EventType
import com.service.accountwebhookserver.repository.AccountRepository
import com.service.accountwebhookserver.repository.WebhookEventRepository
import com.service.accountwebhookserver.support.DummyModelFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals

class WebhookServiceTest {

    private lateinit var webhookEventRepository: WebhookEventRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var webhookService: WebhookService

    @BeforeEach
    fun setUp() {
        webhookEventRepository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        objectMapper = ObjectMapper()
        webhookService = WebhookService(webhookEventRepository, accountRepository, objectMapper)
    }

    @Test
    fun `동일 eventId 재전송 시 중복 처리 방지`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val request = DummyModelFactory.generateWebhookRequest(
            eventType = EventType.ACCOUNT_DELETED,
        )
        every { webhookEventRepository.existsByEventId(eventId) } returns true

        // When
        val result = webhookService.receiveWebhook(eventId, request)

        // Then
        assertEquals("duplicated", result.status)
        assertEquals("이미 수신된 이벤트입니다", result.message)
        verify(exactly = 0) { webhookEventRepository.save(any(), any(), any()) }
    }

    @Test
    fun `새로운 이벤트 수신 시 저장 후 처리`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val request = DummyModelFactory.generateWebhookRequest(
            eventType = EventType.ACCOUNT_DELETED,
        )
        every { webhookEventRepository.existsByEventId(eventId) } returns false
        every { webhookEventRepository.save(any(), any(), any()) } returns DummyModelFactory.generateEventResponse(eventId = eventId)
        every { accountRepository.updateStatus(any(), any()) } returns DummyModelFactory.generateAccountResponse()

        // When
        val result = webhookService.receiveWebhook(eventId, request)

        // Then
        assertEquals("processed", result.status)
        verify { webhookEventRepository.save(eventId, "ACCOUNT_DELETED", any()) }
    }

    @Test
    fun `EMAIL_FORWARDING_CHANGED 처리 후 이메일 업데이트`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val accountKey = DummyModelFactory.generateAccountKey()
        val newEmail = DummyModelFactory.generateEmail()
        val request = DummyModelFactory.generateWebhookRequest(
            accountKey = accountKey,
            eventType = EventType.EMAIL_FORWARDING_CHANGED,
            data = mapOf("email" to newEmail),
        )
        every { webhookEventRepository.existsByEventId(eventId) } returns false
        every { webhookEventRepository.save(any(), any(), any()) } returns DummyModelFactory.generateEventResponse(eventId = eventId)
        every { accountRepository.updateEmail(accountKey, newEmail) } returns DummyModelFactory.generateAccountResponse(email = newEmail)

        // When
        val result = webhookService.receiveWebhook(eventId, request)

        // Then
        assertEquals("processed", result.status)
        verify { accountRepository.updateEmail(accountKey, newEmail) }
        verify { webhookEventRepository.updateStatus(eventId, EventStatus.DONE) }
    }

    @Test
    fun `ACCOUNT_DELETED 처리 후 상태가 DELETED로 변경`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val accountKey = DummyModelFactory.generateAccountKey()
        val request = DummyModelFactory.generateWebhookRequest(
            accountKey = accountKey,
            eventType = EventType.ACCOUNT_DELETED,
        )
        every { webhookEventRepository.existsByEventId(eventId) } returns false
        every { webhookEventRepository.save(any(), any(), any()) } returns DummyModelFactory.generateEventResponse(eventId = eventId)
        every { accountRepository.updateStatus(accountKey, AccountStatus.DELETED) } returns
            DummyModelFactory.generateAccountResponse(status = AccountStatus.DELETED.name)

        // When
        val result = webhookService.receiveWebhook(eventId, request)

        // Then
        assertEquals("processed", result.status)
        verify { accountRepository.updateStatus(accountKey, AccountStatus.DELETED) }
        verify { webhookEventRepository.updateStatus(eventId, EventStatus.DONE) }
    }

    @Test
    fun `APPLE_ACCOUNT_DELETED 처리 후 상태가 APPLE_DELETED로 변경`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val accountKey = DummyModelFactory.generateAccountKey()
        val request = DummyModelFactory.generateWebhookRequest(
            accountKey = accountKey,
            eventType = EventType.APPLE_ACCOUNT_DELETED,
        )
        every { webhookEventRepository.existsByEventId(eventId) } returns false
        every { webhookEventRepository.save(any(), any(), any()) } returns DummyModelFactory.generateEventResponse(eventId = eventId)
        every { accountRepository.updateStatus(accountKey, AccountStatus.APPLE_DELETED) } returns
            DummyModelFactory.generateAccountResponse(status = AccountStatus.APPLE_DELETED.name)

        // When
        val result = webhookService.receiveWebhook(eventId, request)

        // Then
        assertEquals("processed", result.status)
        verify { accountRepository.updateStatus(accountKey, AccountStatus.APPLE_DELETED) }
        verify { webhookEventRepository.updateStatus(eventId, EventStatus.DONE) }
    }

    @Test
    fun `EMAIL_FORWARDING_CHANGED에서 email 누락 시 FAILED 상태`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val request = DummyModelFactory.generateWebhookRequest(
            eventType = EventType.EMAIL_FORWARDING_CHANGED,
            data = null,
        )
        every { webhookEventRepository.existsByEventId(eventId) } returns false
        every { webhookEventRepository.save(any(), any(), any()) } returns DummyModelFactory.generateEventResponse(eventId = eventId)

        // When
        val result = webhookService.receiveWebhook(eventId, request)

        // Then
        assertEquals("failed", result.status)
        verify { webhookEventRepository.updateStatus(eventId, EventStatus.FAILED, "email이 필요합니다") }
    }

    @Test
    fun `존재하지 않는 계정 업데이트 시 FAILED 상태와 error_message 저장`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val accountKey = DummyModelFactory.generateAccountKey()
        val request = DummyModelFactory.generateWebhookRequest(
            accountKey = accountKey,
            eventType = EventType.ACCOUNT_DELETED,
        )
        every { webhookEventRepository.existsByEventId(eventId) } returns false
        every { webhookEventRepository.save(any(), any(), any()) } returns DummyModelFactory.generateEventResponse(eventId = eventId)
        every { accountRepository.updateStatus(accountKey, AccountStatus.DELETED) } returns null

        // When
        val result = webhookService.receiveWebhook(eventId, request)

        // Then
        assertEquals("failed", result.status)
        verify {
            webhookEventRepository.updateStatus(
                eventId,
                EventStatus.FAILED,
                "계정을 찾을 수 없습니다: $accountKey",
            )
        }
    }

    @Test
    fun `이메일 업데이트 시 계정이 없으면 FAILED 상태`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val accountKey = DummyModelFactory.generateAccountKey()
        val newEmail = DummyModelFactory.generateEmail()
        val request = DummyModelFactory.generateWebhookRequest(
            accountKey = accountKey,
            eventType = EventType.EMAIL_FORWARDING_CHANGED,
            data = mapOf("email" to newEmail),
        )
        every { webhookEventRepository.existsByEventId(eventId) } returns false
        every { webhookEventRepository.save(any(), any(), any()) } returns DummyModelFactory.generateEventResponse(eventId = eventId)
        every { accountRepository.updateEmail(accountKey, any()) } returns null

        // When
        val result = webhookService.receiveWebhook(eventId, request)

        // Then
        assertEquals("failed", result.status)
        verify {
            webhookEventRepository.updateStatus(
                eventId,
                EventStatus.FAILED,
                "계정을 찾을 수 없습니다: $accountKey",
            )
        }
    }
}
