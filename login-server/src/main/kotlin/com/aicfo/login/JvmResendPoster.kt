package com.aicfo.login

import java.net.HttpURLConnection
import java.net.URL

/** The only process that talks to Resend. */
class JvmResendPoster : ResendPoster {
    override fun post(secrets: MailSecrets, to: String, code: String): Int {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer ${secrets.apiKey}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            val body = resendJson(secrets.fromAddress, to, code)
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val ENDPOINT = "https://api.resend.com/emails"
    }
}
