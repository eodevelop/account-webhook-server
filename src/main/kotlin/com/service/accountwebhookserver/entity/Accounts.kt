package com.service.accountwebhookserver.entity

import com.service.accountwebhookserver.common.AccountStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object Accounts : Table("account") {
    val id = long("id").autoIncrement()
    val accountKey = varchar("account_key", 255).uniqueIndex()
    val email = varchar("email", 255)
    val status = varchar("status", 50).default(AccountStatus.ACTIVE.name)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
