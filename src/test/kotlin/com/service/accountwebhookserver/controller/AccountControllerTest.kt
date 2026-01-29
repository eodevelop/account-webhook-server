package com.service.accountwebhookserver.controller

import com.ninjasquad.springmockk.MockkBean
import com.service.accountwebhookserver.common.AccountStatus
import com.service.accountwebhookserver.model.AccountResponse
import com.service.accountwebhookserver.model.CreateAccountRequest
import com.service.accountwebhookserver.service.AccountService
import com.service.accountwebhookserver.util.SignatureValidator
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@WebMvcTest(AccountController::class)
class AccountControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var accountService: AccountService

    @MockkBean
    private lateinit var signatureValidator: SignatureValidator

    private val objectMapper = ObjectMapper()

    @Test
    fun `계정 생성 성공 시 201 Created 반환`() {
        // Given
        val request = CreateAccountRequest(
            accountKey = "acc-12345678",
            email = "test@example.com",
        )
        val response = AccountResponse(
            accountKey = "acc-12345678",
            email = "test@example.com",
            status = AccountStatus.ACTIVE.name,
        )
        every { accountService.createAccount(any()) } returns response

        // When & Then
        mockMvc.post("/accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accountKey") { value("acc-12345678") }
            jsonPath("$.email") { value("test@example.com") }
            jsonPath("$.status") { value("ACTIVE") }
        }
    }

    @Test
    fun `존재하는 계정 조회 시 200 OK 반환`() {
        // Given
        val accountKey = "acc-12345678"
        val response = AccountResponse(
            accountKey = accountKey,
            email = "test@example.com",
            status = AccountStatus.ACTIVE.name,
        )
        every { accountService.getAccount(accountKey) } returns response

        // When & Then
        mockMvc.get("/accounts/{accountKey}", accountKey)
            .andExpect {
                status { isOk() }
                jsonPath("$.accountKey") { value(accountKey) }
                jsonPath("$.email") { value("test@example.com") }
                jsonPath("$.status") { value("ACTIVE") }
            }
    }

    @Test
    fun `존재하지 않는 계정 조회 시 404 Not Found 반환`() {
        // Given
        val accountKey = "acc-nonexistent"
        every { accountService.getAccount(accountKey) } returns null

        // When & Then
        mockMvc.get("/accounts/{accountKey}", accountKey)
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `DELETED 상태 계정 조회`() {
        // Given
        val accountKey = "acc-deleted"
        val response = AccountResponse(
            accountKey = accountKey,
            email = "deleted@example.com",
            status = AccountStatus.DELETED.name,
        )
        every { accountService.getAccount(accountKey) } returns response

        // When & Then
        mockMvc.get("/accounts/{accountKey}", accountKey)
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("DELETED") }
            }
    }
}
