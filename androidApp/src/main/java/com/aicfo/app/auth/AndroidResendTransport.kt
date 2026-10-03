package com.aicfo.app.auth

import com.aicfo.shared.auth.ResendApi
import com.aicfo.shared.auth.ResendHttpResult
import com.aicfo.shared.auth.ResendTransport
import com.aicfo.shared.security.TlsPolicy
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTPS POST to Resend. The call runs off the caller thread.
 * The API key and the message body are not written to logs.
 */
class AndroidResendTransport : ResendTransport {
    override fun postEmail(apiKey: String, jsonBody: String): ResendHttpResult {
        TlsPolicy.requireHttps(ResendApi.ENDPOINT)
        val holder = arrayOfNulls<ResendHttpResult>(1)
        val worker = Thread {
            holder[0] = post(apiKey, jsonBody)
        }
        worker.start()
        worker.join(20_000)
        if (worker.isAlive) {
            worker.interrupt()
            return ResendHttpResult(0, "Couldn't send the code.")
        }
        return holder[0] ?: ResendHttpResult(0, "Couldn't send the code.")
    }

    private fun post(apiKey: String, jsonBody: String): ResendHttpResult {
        val connection = (URL(ResendApi.ENDPOINT).openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { stream ->
                stream.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            if (status in 200..299) ResendHttpResult(status) else ResendHttpResult(status, "Couldn't send the code.")
        } catch (_: Exception) {
            ResendHttpResult(0, "Couldn't send the code.")
        } finally {
            connection.disconnect()
        }
    }
}
