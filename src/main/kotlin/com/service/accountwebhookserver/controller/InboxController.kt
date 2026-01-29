package com.service.accountwebhookserver.controller

import com.service.accountwebhookserver.model.EventResponse
import com.service.accountwebhookserver.service.EventService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/inbox/events")
class InboxController(
    private val eventService: EventService,
) {

    @GetMapping("/{eventId}")
    fun getEvent(@PathVariable eventId: String): ResponseEntity<EventResponse> {
        val event = eventService.getEvent(eventId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(event)
    }
}
