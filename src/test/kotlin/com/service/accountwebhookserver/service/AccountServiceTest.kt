package com.service.accountwebhookserver.service

import com.service.accountwebhookserver.common.AccountStatus
import com.service.accountwebhookserver.repository.AccountRepository
import com.service.accountwebhookserver.support.DummyModelFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountServiceTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var accountService: AccountService

    @BeforeEach
    fun setUp() {
        accountRepository = mockk(relaxed = true)
        accountService = AccountService(accountRepository)
    }

    @Test
    fun `계정 생성 성공`() {
        // Given
        val request = DummyModelFactory.generateCreateAccountRequest()
        val expectedResponse = DummyModelFactory.generateAccountResponse(
            accountKey = request.accountKey,
            email = request.email,
            status = AccountStatus.ACTIVE.name,
        )
        every { accountRepository.save(request.accountKey, request.email) } returns expectedResponse

        // When
        val result = accountService.createAccount(request)

        // Then
        assertEquals(request.accountKey, result.accountKey)
        assertEquals(request.email, result.email)
        assertEquals(AccountStatus.ACTIVE.name, result.status)
        verify { accountRepository.save(request.accountKey, request.email) }
    }

    @Test
    fun `존재하는 계정 조회 성공`() {
        // Given
        val accountKey = DummyModelFactory.generateAccountKey()
        val expectedResponse = DummyModelFactory.generateAccountResponse(accountKey = accountKey)
        every { accountRepository.findByAccountKey(accountKey) } returns expectedResponse

        // When
        val result = accountService.getAccount(accountKey)

        // Then
        assertEquals(accountKey, result?.accountKey)
        assertEquals(expectedResponse.email, result?.email)
        verify { accountRepository.findByAccountKey(accountKey) }
    }

    @Test
    fun `존재하지 않는 계정 조회 시 null 반환`() {
        // Given
        val accountKey = DummyModelFactory.generateAccountKey()
        every { accountRepository.findByAccountKey(accountKey) } returns null

        // When
        val result = accountService.getAccount(accountKey)

        // Then
        assertNull(result)
        verify { accountRepository.findByAccountKey(accountKey) }
    }
}
