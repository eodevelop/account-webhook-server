package com.service.accountwebhookserver.config

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.ByteArrayInputStream

class CachedBodyHttpServletRequest(
    request: HttpServletRequest,
) : HttpServletRequestWrapper(request) {

    private val cachedBody: ByteArray = request.inputStream.readAllBytes()

    override fun getInputStream(): ServletInputStream {
        return CachedBodyServletInputStream(cachedBody)
    }

    fun getBodyString(): String = String(cachedBody, Charsets.UTF_8)

    private class CachedBodyServletInputStream(
        private val cachedBody: ByteArray,
    ) : ServletInputStream() {

        private val inputStream = ByteArrayInputStream(cachedBody)

        override fun read(): Int = inputStream.read()

        override fun isFinished(): Boolean = inputStream.available() == 0

        override fun isReady(): Boolean = true

        override fun setReadListener(listener: ReadListener?) {
            throw UnsupportedOperationException()
        }
    }
}
