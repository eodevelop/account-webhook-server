package com.service.accountwebhookserver.repository

import com.service.accountwebhookserver.common.AccountStatus
import com.service.accountwebhookserver.entity.Accounts
import com.service.accountwebhookserver.model.AccountResponse
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class AccountRepository {

    fun save(accountKey: String, email: String): AccountResponse = transaction {
        Accounts.insert {
            it[Accounts.accountKey] = accountKey
            it[Accounts.email] = email
            it[status] = AccountStatus.ACTIVE.name
        }
        findByAccountKeyInternal(accountKey)!!
    }

    fun updateEmail(accountKey: String, email: String): AccountResponse? = transaction {
        val updated = Accounts.update({ Accounts.accountKey eq accountKey }) {
            it[Accounts.email] = email
            it[updatedAt] = Instant.now()
        }
        if (updated > 0) findByAccountKeyInternal(accountKey) else null
    }

    fun updateStatus(accountKey: String, status: AccountStatus): AccountResponse? = transaction {
        val updated = Accounts.update({ Accounts.accountKey eq accountKey }) {
            it[Accounts.status] = status.name
            it[updatedAt] = Instant.now()
        }
        if (updated > 0) findByAccountKeyInternal(accountKey) else null
    }

    fun findByAccountKey(accountKey: String): AccountResponse? = transaction {
        findByAccountKeyInternal(accountKey)
    }

    fun existsByAccountKey(accountKey: String): Boolean = transaction {
        Accounts.select(Accounts.id)
            .where { Accounts.accountKey eq accountKey }
            .count() > 0
    }

    private fun findByAccountKeyInternal(accountKey: String): AccountResponse? {
        return Accounts.select(Accounts.columns)
            .where { Accounts.accountKey eq accountKey }
            .map { it.toAccountResponse() }
            .singleOrNull()
    }

    private fun ResultRow.toAccountResponse() = AccountResponse(
        accountKey = this[Accounts.accountKey],
        email = this[Accounts.email],
        status = this[Accounts.status],
    )
}
