package com.service.accountwebhookserver.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class SignatureValidator(
    @Value("\${webhook.secret}") private val secret: String,
) {

    fun verify(payload: String, signature: String): Boolean {
        val computed = computeSignature(payload)
        return computed == signature
    }

    private fun computeSignature(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val hash = mac.doFinal(payload.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }
}
