package com.service.accountwebhookserver.service

import com.service.accountwebhookserver.common.EventStatus
import com.service.accountwebhookserver.common.EventType
import com.service.accountwebhookserver.repository.WebhookEventRepository
import com.service.accountwebhookserver.support.DummyModelFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventServiceTest {

    private lateinit var webhookEventRepository: WebhookEventRepository
    private lateinit var eventService: EventService

    @BeforeEach
    fun setUp() {
        webhookEventRepository = mockk(relaxed = true)
        eventService = EventService(webhookEventRepository)
    }

    @Test
    fun `존재하는 이벤트 조회 성공`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val expectedResponse = DummyModelFactory.generateEventResponse(
            eventId = eventId,
            eventType = EventType.ACCOUNT_DELETED.name,
            status = EventStatus.DONE.name,
            processedAt = Instant.now(),
        )
        every { webhookEventRepository.findByEventId(eventId) } returns expectedResponse

        // When
        val result = eventService.getEvent(eventId)

        // Then
        assertEquals(eventId, result?.eventId)
        assertEquals(EventType.ACCOUNT_DELETED.name, result?.eventType)
        assertEquals(EventStatus.DONE.name, result?.status)
        verify { webhookEventRepository.findByEventId(eventId) }
    }

    @Test
    fun `존재하지 않는 이벤트 조회 시 null 반환`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        every { webhookEventRepository.findByEventId(eventId) } returns null

        // When
        val result = eventService.getEvent(eventId)

        // Then
        assertNull(result)
        verify { webhookEventRepository.findByEventId(eventId) }
    }

    @Test
    fun `PROCESSING 상태 이벤트 조회`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val expectedResponse = DummyModelFactory.generateEventResponse(
            eventId = eventId,
            eventType = EventType.EMAIL_FORWARDING_CHANGED.name,
            status = EventStatus.PROCESSING.name,
            processedAt = null,
        )
        every { webhookEventRepository.findByEventId(eventId) } returns expectedResponse

        // When
        val result = eventService.getEvent(eventId)

        // Then
        assertEquals(EventStatus.PROCESSING.name, result?.status)
        assertNull(result?.processedAt)
    }

    @Test
    fun `FAILED 상태 이벤트 조회`() {
        // Given
        val eventId = DummyModelFactory.generateEventId()
        val expectedResponse = DummyModelFactory.generateEventResponse(
            eventId = eventId,
            eventType = EventType.ACCOUNT_DELETED.name,
            status = EventStatus.FAILED.name,
            processedAt = Instant.now(),
        )
        every { webhookEventRepository.findByEventId(eventId) } returns expectedResponse

        // When
        val result = eventService.getEvent(eventId)

        // Then
        assertEquals(EventStatus.FAILED.name, result?.status)
    }
}
