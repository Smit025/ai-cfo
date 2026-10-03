package com.aicfo.app.auth

import com.aicfo.shared.auth.EmailAuthRepository
import com.aicfo.shared.auth.LoginMail

/**
 * Release reads the login server URL from the process environment.
 * Debug builds do not call the server. The mail key is not read here.
 */
object LoginServerConfig {
    const val URL_ENV = "FINWISE_LOGIN_SERVER_URL"

    fun repository(debugBuild: Boolean): EmailAuthRepository {
        val url = if (debugBuild) "" else System.getenv(URL_ENV)?.trim().orEmpty()
        return LoginMail.repository(
            debugBuild = debugBuild,
            serverUrl = url,
            transport = AndroidLoginServerTransport(),
        )
    }
}
