package com.aicfo.app.auth

import com.aicfo.shared.auth.LoginHttpResult
import com.aicfo.shared.auth.LoginServerTransport
import com.aicfo.shared.security.TlsPolicy
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTPS POST to the login server. The call runs off the caller thread.
 * This client does not send a mail-provider key.
 */
class AndroidLoginServerTransport : LoginServerTransport {
    override fun post(url: String, jsonBody: String): LoginHttpResult {
        TlsPolicy.requireHttps(url)
        val holder = arrayOfNulls<LoginHttpResult>(1)
        val worker = Thread {
            holder[0] = postOnWorker(url, jsonBody)
        }
        worker.start()
        worker.join(20_000)
        if (worker.isAlive) {
            worker.interrupt()
            return LoginHttpResult(0, "Couldn't send the code.")
        }
        return holder[0] ?: LoginHttpResult(0, "Couldn't send the code.")
    }

    private fun postOnWorker(url: String, jsonBody: String): LoginHttpResult {
        val connection = (URL(url).openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { stream ->
                stream.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            if (status in 200..299) {
                LoginHttpResult(status)
            } else {
                LoginHttpResult(status, "Couldn't send the code.")
            }
        } catch (_: Exception) {
            LoginHttpResult(0, "Couldn't send the code.")
        } finally {
            connection.disconnect()
        }
    }
}
