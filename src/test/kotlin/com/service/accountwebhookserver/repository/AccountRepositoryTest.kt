package com.service.accountwebhookserver.repository

import com.service.accountwebhookserver.common.AccountStatus
import com.service.accountwebhookserver.entity.Accounts
import com.service.accountwebhookserver.support.DummyModelFactory
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccountRepositoryTest {

    private lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun setUp() {
        Database.connect(
            url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
        )
        transaction {
            SchemaUtils.create(Accounts)
        }
        accountRepository = AccountRepository()
    }

    @AfterEach
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Accounts)
        }
    }

    @Test
    fun `계정 저장 성공`() {
        // Given
        val accountKey = DummyModelFactory.generateAccountKey()
        val email = DummyModelFactory.generateEmail()

        // When
        val result = accountRepository.save(accountKey, email)

        // Then
        assertEquals(accountKey, result.accountKey)
        assertEquals(email, result.email)
        assertEquals(AccountStatus.ACTIVE.name, result.status)
    }

    @Test
    fun `계정 조회 성공`() {
        // Given
        val accountKey = DummyModelFactory.generateAccountKey()
        val email = DummyModelFactory.generateEmail()
        accountRepository.save(accountKey, email)

        // When
        val result = accountRepository.findByAccountKey(accountKey)

        // Then
        assertNotNull(result)
        assertEquals(accountKey, result.accountKey)
        assertEquals(email, result.email)
    }

    @Test
    fun `존재하지 않는 계정 조회 시 null 반환`() {
        // Given
        val nonExistentKey = DummyModelFactory.generateAccountKey()

        // When
        val result = accountRepository.findByAccountKey(nonExistentKey)

        // Then
        assertNull(result)
    }

    @Test
    fun `이메일 업데이트 성공`() {
        // Given
        val accountKey = DummyModelFactory.generateAccountKey()
        val originalEmail = DummyModelFactory.generateEmail()
        val newEmail = DummyModelFactory.generateEmail()
        accountRepository.save(accountKey, originalEmail)

        // When
        val result = accountRepository.updateEmail(accountKey, newEmail)

        // Then
        assertNotNull(result)
        assertEquals(newEmail, result.email)
    }

    @Test
    fun `존재하지 않는 계정 이메일 업데이트 시 null 반환`() {
        // Given
        val nonExistentKey = DummyModelFactory.generateAccountKey()
        val newEmail = DummyModelFactory.generateEmail()

        // When
        val result = accountRepository.updateEmail(nonExistentKey, newEmail)

        // Then
        assertNull(result)
    }

    @Test
    fun `계정 상태를 DELETED로 변경`() {
        // Given
        val accountKey = DummyModelFactory.generateAccountKey()
        val email = DummyModelFactory.generateEmail()
        accountRepository.save(accountKey, email)

        // When
        val result = accountRepository.updateStatus(accountKey, AccountStatus.DELETED)

        // Then
        assertNotNull(result)
        assertEquals(AccountStatus.DELETED.name, result.status)
    }

    @Test
    fun `계정 상태를 APPLE_DELETED로 변경`() {
        // Given
        val accountKey = DummyModelFactory.generateAccountKey()
        val email = DummyModelFactory.generateEmail()
        accountRepository.save(accountKey, email)

        // When
        val result = accountRepository.updateStatus(accountKey, AccountStatus.APPLE_DELETED)

        // Then
        assertNotNull(result)
        assertEquals(AccountStatus.APPLE_DELETED.name, result.status)
    }

    @Test
    fun `존재하지 않는 계정 상태 업데이트 시 null 반환`() {
        // Given
        val nonExistentKey = DummyModelFactory.generateAccountKey()

        // When
        val result = accountRepository.updateStatus(nonExistentKey, AccountStatus.DELETED)

        // Then
        assertNull(result)
    }
}
