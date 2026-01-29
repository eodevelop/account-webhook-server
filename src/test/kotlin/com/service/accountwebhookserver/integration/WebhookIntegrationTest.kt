package com.service.accountwebhookserver.integration

import com.service.accountwebhookserver.common.AccountStatus
import com.service.accountwebhookserver.common.EventStatus
import com.service.accountwebhookserver.common.EventType
import com.service.accountwebhookserver.model.CreateAccountRequest
import com.service.accountwebhookserver.model.WebhookRequest
import com.service.accountwebhookserver.support.DummyModelFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Value("\${webhook.secret}")
    private lateinit var webhookSecret: String

    private val objectMapper = ObjectMapper()

    private fun computeSignature(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val hash = mac.doFinal(payload.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    @Test
    fun `계정 생성부터 삭제까지 전체 플로우 테스트`() {
        val accountKey = DummyModelFactory.generateAccountKey()
        val email = DummyModelFactory.generateEmail()

        // 1. 계정 생성
        val createRequest = CreateAccountRequest(accountKey, email)
        mockMvc.post("/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accountKey") { value(accountKey) }
            jsonPath("$.status") { value(AccountStatus.ACTIVE.name) }
        }

        // 2. 계정 조회 확인
        mockMvc.get("/accounts/{accountKey}", accountKey)
            .andExpect {
                status { isOk() }
                jsonPath("$.accountKey") { value(accountKey) }
                jsonPath("$.email") { value(email) }
                jsonPath("$.status") { value(AccountStatus.ACTIVE.name) }
            }

        // 3. ACCOUNT_DELETED 웹훅 수신
        val eventId = DummyModelFactory.generateEventId()
        val webhookRequest = WebhookRequest(
            accountKey = accountKey,
            eventType = EventType.ACCOUNT_DELETED,
            data = null,
        )
        val payload = objectMapper.writeValueAsString(webhookRequest)
        val signature = computeSignature(payload)

        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", eventId)
            header("X-Signature", signature)
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("processed") }
        }

        // 4. 계정 상태가 DELETED로 변경되었는지 확인
        mockMvc.get("/accounts/{accountKey}", accountKey)
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value(AccountStatus.DELETED.name) }
            }

        // 5. 이벤트 상태가 DONE인지 확인
        mockMvc.get("/inbox/events/{eventId}", eventId)
            .andExpect {
                status { isOk() }
                jsonPath("$.eventId") { value(eventId) }
                jsonPath("$.status") { value(EventStatus.DONE.name) }
            }
    }

    @Test
    fun `이메일 변경 웹훅 처리 플로우 테스트`() {
        val accountKey = DummyModelFactory.generateAccountKey()
        val originalEmail = DummyModelFactory.generateEmail()
        val newEmail = DummyModelFactory.generateEmail()

        // 1. 계정 생성
        val createRequest = CreateAccountRequest(accountKey, originalEmail)
        mockMvc.post("/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isCreated() }
        }

        // 2. EMAIL_FORWARDING_CHANGED 웹훅 수신
        val eventId = DummyModelFactory.generateEventId()
        val webhookRequest = WebhookRequest(
            accountKey = accountKey,
            eventType = EventType.EMAIL_FORWARDING_CHANGED,
            data = mapOf("email" to newEmail),
        )
        val payload = objectMapper.writeValueAsString(webhookRequest)
        val signature = computeSignature(payload)

        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", eventId)
            header("X-Signature", signature)
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("processed") }
        }

        // 3. 이메일이 변경되었는지 확인
        mockMvc.get("/accounts/{accountKey}", accountKey)
            .andExpect {
                status { isOk() }
                jsonPath("$.email") { value(newEmail) }
            }
    }

    @Test
    fun `중복 이벤트 처리 방지 테스트`() {
        val accountKey = DummyModelFactory.generateAccountKey()
        val email = DummyModelFactory.generateEmail()

        // 1. 계정 생성
        mockMvc.post("/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateAccountRequest(accountKey, email))
        }

        // 2. 첫 번째 웹훅 수신
        val eventId = DummyModelFactory.generateEventId()
        val webhookRequest = WebhookRequest(
            accountKey = accountKey,
            eventType = EventType.ACCOUNT_DELETED,
            data = null,
        )
        val payload = objectMapper.writeValueAsString(webhookRequest)
        val signature = computeSignature(payload)

        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", eventId)
            header("X-Signature", signature)
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("processed") }
        }

        // 3. 동일한 eventId로 재전송 시 duplicated 응답
        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", eventId)
            header("X-Signature", signature)
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("duplicated") }
        }
    }

    @Test
    fun `서명 검증 실패 테스트`() {
        val webhookRequest = WebhookRequest(
            accountKey = "acc-12345678",
            eventType = EventType.ACCOUNT_DELETED,
            data = null,
        )
        val payload = objectMapper.writeValueAsString(webhookRequest)

        // 잘못된 서명으로 요청
        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", "evt-12345678")
            header("X-Signature", "invalid-signature")
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `존재하지 않는 계정에 대한 웹훅 처리 시 FAILED 상태`() {
        val nonExistentAccountKey = DummyModelFactory.generateAccountKey()
        val eventId = DummyModelFactory.generateEventId()

        val webhookRequest = WebhookRequest(
            accountKey = nonExistentAccountKey,
            eventType = EventType.ACCOUNT_DELETED,
            data = null,
        )
        val payload = objectMapper.writeValueAsString(webhookRequest)
        val signature = computeSignature(payload)

        // 1. 존재하지 않는 계정에 대한 웹훅 수신
        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", eventId)
            header("X-Signature", signature)
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("failed") }
        }

        // 2. 이벤트 상태가 FAILED인지 확인
        mockMvc.get("/inbox/events/{eventId}", eventId)
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value(EventStatus.FAILED.name) }
            }
    }

    @Test
    fun `APPLE_ACCOUNT_DELETED 웹훅 처리 테스트`() {
        val accountKey = DummyModelFactory.generateAccountKey()
        val email = DummyModelFactory.generateEmail()

        // 1. 계정 생성
        mockMvc.post("/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateAccountRequest(accountKey, email))
        }

        // 2. APPLE_ACCOUNT_DELETED 웹훅 수신
        val eventId = DummyModelFactory.generateEventId()
        val webhookRequest = WebhookRequest(
            accountKey = accountKey,
            eventType = EventType.APPLE_ACCOUNT_DELETED,
            data = null,
        )
        val payload = objectMapper.writeValueAsString(webhookRequest)
        val signature = computeSignature(payload)

        mockMvc.post("/webhooks/account-changes") {
            header("X-Event-Id", eventId)
            header("X-Signature", signature)
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("processed") }
        }

        // 3. 계정 상태가 APPLE_DELETED로 변경되었는지 확인
        mockMvc.get("/accounts/{accountKey}", accountKey)
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value(AccountStatus.APPLE_DELETED.name) }
            }
    }

    @Test
    fun `존재하지 않는 계정 조회 시 404 반환`() {
        val nonExistentAccountKey = DummyModelFactory.generateAccountKey()

        mockMvc.get("/accounts/{accountKey}", nonExistentAccountKey)
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `존재하지 않는 이벤트 조회 시 404 반환`() {
        val nonExistentEventId = DummyModelFactory.generateEventId()

        mockMvc.get("/inbox/events/{eventId}", nonExistentEventId)
            .andExpect {
                status { isNotFound() }
            }
    }
}
