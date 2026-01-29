package com.service.accountwebhookserver.config

import com.service.accountwebhookserver.util.SignatureValidator
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class SignatureInterceptor(
    private val signatureValidator: SignatureValidator,
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val signature = request.getHeader("X-Signature")
        if (signature.isNullOrBlank()) {
            response.sendError(401, "X-Signature 헤더 누락")
            return false
        }

        val cachedRequest = request as? CachedBodyHttpServletRequest
            ?: run {
                response.sendError(500, "Request body를 읽을 수 없음")
                return false
            }

        val body = cachedRequest.getBodyString()
        if (!signatureValidator.verify(body, signature)) {
            response.sendError(401, "서명 검증 실패")
            return false
        }

        return true
    }
}
