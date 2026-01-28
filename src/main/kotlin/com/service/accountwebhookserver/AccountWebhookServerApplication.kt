package com.service.accountwebhookserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AccountWebhookServerApplication

fun main(args: Array<String>) {
    runApplication<AccountWebhookServerApplication>(*args)
}
