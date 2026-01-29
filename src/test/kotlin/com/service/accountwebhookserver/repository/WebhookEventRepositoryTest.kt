package com.service.accountwebhookserver.repository

import com.service.accountwebhookserver.common.EventStatus
import com.service.accountwebhookserver.common.EventType
import com.service.accountwebhookserver.entity.WebhookEvents
import com.service.accountwebhookserver.support.DummyModelFactory
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebhookEventRepositoryTest {

    private lateinit var webhookEventRepository: WebhookEventRepository

    @BeforeEach
    fun setUp() {
        Database.connect(
            url = "jdbc:h2:mem:testdb2;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
        )
        transaction {
            SchemaUtils.create(WebhookEvents)
        }
        webhookEventRepository = WebhookEventRepository()
    }

    @AfterEach
    fun tearDown() {
        transaction {
            SchemaUtils.drop(WebhookEvents)
        }
    }

    @Test
    fun `이벤트 저장 성공`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val eventType = EventType.ACCOUNT_DELETED.name
        val payload = """{"accountKey": "acc-12345678"}"""

        // When
        val result = webhookEventRepository.save(eventId, eventType, payload)

        // Then
        assertEquals(eventId, result.eventId)
        assertEquals(eventType, result.eventType)
        assertEquals(EventStatus.RECEIVED.name, result.status)
        assertNull(result.processedAt)
    }

    @Test
    fun `이벤트 조회 성공`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val eventType = EventType.EMAIL_FORWARDING_CHANGED.name
        val payload = """{"accountKey": "acc-12345678", "email": "new@example.com"}"""
        webhookEventRepository.save(eventId, eventType, payload)

        // When
        val result = webhookEventRepository.findByEventId(eventId)

        // Then
        assertNotNull(result)
        assertEquals(eventId, result.eventId)
        assertEquals(eventType, result.eventType)
    }

    @Test
    fun `존재하지 않는 이벤트 조회 시 null 반환`() {
        // Given
        val nonExistentEventId = DummyModelFactory.generateEventId()

        // When
        val result = webhookEventRepository.findByEventId(nonExistentEventId)

        // Then
        assertNull(result)
    }

    @Test
    fun `이벤트 존재 여부 확인 - 존재하는 경우`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        webhookEventRepository.save(eventId, EventType.ACCOUNT_DELETED.name, "{}")

        // When
        val result = webhookEventRepository.existsByEventId(eventId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `이벤트 존재 여부 확인 - 존재하지 않는 경우`() {
        // Given
        val nonExistentEventId = DummyModelFactory.generateEventId()

        // When
        val result = webhookEventRepository.existsByEventId(nonExistentEventId)

        // Then
        assertFalse(result)
    }

    @Test
    fun `이벤트 상태를 PROCESSING으로 변경`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        webhookEventRepository.save(eventId, EventType.ACCOUNT_DELETED.name, "{}")

        // When
        val result = webhookEventRepository.updateStatus(eventId, EventStatus.PROCESSING)

        // Then
        assertNotNull(result)
        assertEquals(EventStatus.PROCESSING.name, result.status)
        assertNull(result.processedAt)
    }

    @Test
    fun `이벤트 상태를 DONE으로 변경 시 processedAt 설정`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        webhookEventRepository.save(eventId, EventType.ACCOUNT_DELETED.name, "{}")

        // When
        val result = webhookEventRepository.updateStatus(eventId, EventStatus.DONE)

        // Then
        assertNotNull(result)
        assertEquals(EventStatus.DONE.name, result.status)
        assertNotNull(result.processedAt)
    }

    @Test
    fun `이벤트 상태를 FAILED로 변경 시 errorMessage 저장`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val errorMessage = "계정을 찾을 수 없습니다"
        webhookEventRepository.save(eventId, EventType.ACCOUNT_DELETED.name, "{}")

        // When
        val result = webhookEventRepository.updateStatus(eventId, EventStatus.FAILED, errorMessage)

        // Then
        assertNotNull(result)
        assertEquals(EventStatus.FAILED.name, result.status)
        assertNotNull(result.processedAt)
    }

    @Test
    fun `존재하지 않는 이벤트 상태 업데이트 시 null 반환`() {
        // Given
        val nonExistentEventId = DummyModelFactory.generateEventId()

        // When
        val result = webhookEventRepository.updateStatus(nonExistentEventId, EventStatus.DONE)

        // Then
        assertNull(result)
    }

    @Test
    fun `여러 이벤트 저장 후 각각 조회`() {
        // Given
        val eventId1 = DummyModelFactory.generateEventId()
        val eventId2 = DummyModelFactory.generateEventId()
        webhookEventRepository.save(eventId1, EventType.ACCOUNT_DELETED.name, "{}")
        webhookEventRepository.save(eventId2, EventType.APPLE_ACCOUNT_DELETED.name, "{}")

        // When
        val result1 = webhookEventRepository.findByEventId(eventId1)
        val result2 = webhookEventRepository.findByEventId(eventId2)

        // Then
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(EventType.ACCOUNT_DELETED.name, result1.eventType)
        assertEquals(EventType.APPLE_ACCOUNT_DELETED.name, result2.eventType)
    }
}
