package com.service.accountwebhookserver.controller

import com.ninjasquad.springmockk.MockkBean
import com.service.accountwebhookserver.common.EventStatus
import com.service.accountwebhookserver.common.EventType
import com.service.accountwebhookserver.model.EventResponse
import com.service.accountwebhookserver.service.EventService
import com.service.accountwebhookserver.util.SignatureValidator
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@WebMvcTest(InboxController::class)
class InboxControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var eventService: EventService

    @MockkBean
    private lateinit var signatureValidator: SignatureValidator

    @Test
    fun `존재하는 이벤트 조회 시 200 OK 반환`() {
        // Given
        val eventId = "evt-12345678"
        val response = EventResponse(
            eventId = eventId,
            eventType = EventType.ACCOUNT_DELETED.name,
            status = EventStatus.DONE.name,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            processedAt = Instant.parse("2025-01-01T00:00:01Z"),
        )
        every { eventService.getEvent(eventId) } returns response

        // When & Then
        mockMvc.get("/inbox/events/{eventId}", eventId)
            .andExpect {
                status { isOk() }
                jsonPath("$.eventId") { value(eventId) }
                jsonPath("$.eventType") { value("ACCOUNT_DELETED") }
                jsonPath("$.status") { value("DONE") }
            }
    }

    @Test
    fun `존재하지 않는 이벤트 조회 시 404 Not Found 반환`() {
        // Given
        val eventId = "evt-nonexistent"
        every { eventService.getEvent(eventId) } returns null

        // When & Then
        mockMvc.get("/inbox/events/{eventId}", eventId)
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `PROCESSING 상태 이벤트 조회`() {
        // Given
        val eventId = "evt-processing"
        val response = EventResponse(
            eventId = eventId,
            eventType = EventType.EMAIL_FORWARDING_CHANGED.name,
            status = EventStatus.PROCESSING.name,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            processedAt = null,
        )
        every { eventService.getEvent(eventId) } returns response

        // When & Then
        mockMvc.get("/inbox/events/{eventId}", eventId)
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("PROCESSING") }
                jsonPath("$.processedAt") { doesNotExist() }
            }
    }

    @Test
    fun `FAILED 상태 이벤트 조회`() {
        // Given
        val eventId = "evt-failed"
        val response = EventResponse(
            eventId = eventId,
            eventType = EventType.ACCOUNT_DELETED.name,
            status = EventStatus.FAILED.name,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            processedAt = Instant.parse("2025-01-01T00:00:01Z"),
        )
        every { eventService.getEvent(eventId) } returns response

        // When & Then
        mockMvc.get("/inbox/events/{eventId}", eventId)
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("FAILED") }
            }
    }
}
