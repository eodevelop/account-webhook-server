package com.service.accountwebhookserver.config

import com.service.accountwebhookserver.entity.Accounts
import com.service.accountwebhookserver.entity.WebhookEvents
import jakarta.annotation.PostConstruct
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class DatabaseConfig(
    @Value("\${database.url}") private val databaseUrl: String,
) {
    @PostConstruct
    fun init() {
        Database.connect(databaseUrl)

        transaction {
            SchemaUtils.create(Accounts, WebhookEvents)
        }
    }
}
