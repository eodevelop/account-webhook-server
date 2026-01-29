package com.service.accountwebhookserver.service

import com.service.accountwebhookserver.model.AccountResponse
import com.service.accountwebhookserver.model.CreateAccountRequest
import com.service.accountwebhookserver.repository.AccountRepository
import org.springframework.stereotype.Service

@Service
class AccountService(
    private val accountRepository: AccountRepository,
) {

    fun createAccount(request: CreateAccountRequest): AccountResponse {
        return accountRepository.save(request.accountKey, request.email)
    }

    fun getAccount(accountKey: String): AccountResponse? {
        return accountRepository.findByAccountKey(accountKey)
    }
}
