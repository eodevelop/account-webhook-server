package com.service.accountwebhookserver.repository

import com.service.accountwebhookserver.entity.Accounts
import com.service.accountwebhookserver.model.AccountResponse
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

@Repository
class AccountRepository {

    fun findByAccountKey(accountKey: String): AccountResponse? = transaction {
        Accounts.select(Accounts.columns)
            .where { Accounts.accountKey eq  accountKey }
            .map { it.toAccountResponse() }
            .singleOrNull()
    }

    fun existsByAccountKey(accountKey: String): Boolean = transaction {
        Accounts.select(Accounts.id)
            .where { Accounts.accountKey eq accountKey }
            .count() > 0
    }

    private fun ResultRow.toAccountResponse() = AccountResponse(
        accountKey = this[Accounts.accountKey],
        email = this[Accounts.email],
        status = this[Accounts.status],
    )
}
