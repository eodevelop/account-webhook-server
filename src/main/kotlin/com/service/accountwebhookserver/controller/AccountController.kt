package com.service.accountwebhookserver.controller

import com.service.accountwebhookserver.model.AccountResponse
import com.service.accountwebhookserver.model.CreateAccountRequest
import com.service.accountwebhookserver.service.AccountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/accounts")
class AccountController(
    private val accountService: AccountService,
) {

    @PostMapping
    fun createAccount(@RequestBody request: CreateAccountRequest): ResponseEntity<AccountResponse> {
        val account = accountService.createAccount(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(account)
    }

    @GetMapping("/{accountKey}")
    fun getAccount(@PathVariable accountKey: String): ResponseEntity<AccountResponse> {
        val account = accountService.getAccount(accountKey)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(account)
    }
}
