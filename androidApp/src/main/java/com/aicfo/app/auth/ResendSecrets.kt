package com.aicfo.app.auth

import com.aicfo.app.BuildConfig
import com.aicfo.shared.auth.EmailAuthRepository
import com.aicfo.shared.auth.LoginMail
import com.aicfo.shared.auth.MailConfig
import java.io.File

/**
 * Release credentials. Debug builds do not call this.
 *
 * Order: process environment, then a readable `email.local.properties`,
 * then the value Gradle copied from those same places into the release binary.
 * An empty result means the release app stays fail-closed.
 */
object ResendSecrets {
    data class Pairing(val apiKey: String, val fromAddress: String)

    val empty = Pairing("", "")

    fun repository(debugBuild: Boolean): EmailAuthRepository {
        val secrets = if (debugBuild) empty else load()
        return LoginMail.repository(
            debugBuild = debugBuild,
            apiKey = secrets.apiKey,
            fromAddress = secrets.fromAddress,
            transport = AndroidResendTransport(),
        )
    }

    fun load(): Pairing {
        return try {
            val fileText = localFileText()
            val resolved = MailConfig.resolve({ name -> System.getenv(name) }, fileText)
            Pairing(
                apiKey = resolved.first.ifBlank { BuildConfig.RESEND_API_KEY },
                fromAddress = resolved.second.ifBlank { BuildConfig.RESEND_FROM },
            )
        } catch (_: Exception) {
            empty
        }
    }

    private fun localFileText(): String? {
        val pointed = System.getenv("FINWISE_EMAIL_LOCAL_PROPERTIES")?.trim().orEmpty()
        val paths = listOfNotNull(pointed.takeIf { it.isNotEmpty() }, "email.local.properties")
        for (path in paths) {
            val file = File(path)
            if (file.isFile) return file.readText()
        }
        return null
    }
}
