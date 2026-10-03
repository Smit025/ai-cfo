package com.aicfo.login

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

/**
 * Sends the 6-digit login code and checks it. Nothing else.
 * The Resend key stays in this process.
 */
fun main() {
    val port = System.getenv("FINWISE_LOGIN_PORT")?.toIntOrNull() ?: 8787
    val http = LoginHttp(LoginService())
    val server = HttpServer.create(InetSocketAddress("0.0.0.0", port), 0)
    http.attach(server)
    server.start()
}

class LoginHttp(private val service: LoginService) {
    fun attach(server: HttpServer) {
        server.createContext("/login/code") { exchange ->
            handle(exchange, verify = false)
        }
        server.createContext("/login/verify") { exchange ->
            handle(exchange, verify = true)
        }
    }

    private fun handle(exchange: HttpExchange, verify: Boolean) {
        try {
            if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
                respond(exchange, 405)
                return
            }
            val body = exchange.requestBody.use { it.readBytes() }.toString(Charsets.UTF_8)
            val email = jsonStringField(body, "email").orEmpty()
            val result = if (verify) {
                service.verify(email, jsonStringField(body, "code").orEmpty())
            } else {
                service.requestCode(email)
            }
            respond(exchange, result.status)
        } catch (_: Exception) {
            respond(exchange, 400)
        } finally {
            exchange.close()
        }
    }

    private fun respond(exchange: HttpExchange, status: Int) {
        val payload = if (status in 200..299) """{"ok":true}""" else """{"ok":false}"""
        val bytes = payload.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}

class LoginService(
    private val secrets: () -> MailSecrets = { MailSecrets.load() },
    private val resend: ResendPoster = JvmResendPoster(),
    private val newCode: () -> String = { sixDigitCode() },
) {
    private val pending = mutableMapOf<String, String>()
    private var sends: Int = 0

    fun requestCode(email: String): Status {
        val address = normalizeEmail(email) ?: return Status(400)
        val mail = secrets()
        if (!mail.configured()) return Status(503)
        if (sends >= 5) return Status(429)
        val code = freshCode()
        val posted = try {
            resend.post(mail, address, code)
        } catch (_: Exception) {
            return Status(502)
        }
        if (posted !in 200..299) return Status(502)
        sends += 1
        pending[address] = code
        return Status(200)
    }

    fun verify(email: String, code: String): Status {
        if (code == DEBUG_CODE) return Status(400)
        val address = normalizeEmail(email) ?: return Status(400)
        val digits = code.filter { it.isDigit() }
        if (digits.length != 6) return Status(400)
        val expected = pending[address] ?: return Status(400)
        if (digits != expected) return Status(400)
        pending.remove(address)
        return Status(200)
    }

    private fun freshCode(): String {
        repeat(5) {
            val code = newCode().trim()
            if (code != DEBUG_CODE && code.length == 6 && code.all { it.isDigit() }) return code
        }
        return "100001"
    }
}

data class Status(val status: Int)

fun interface ResendPoster {
    /** HTTPS POST to Resend. Must not log [MailSecrets.apiKey] or [code]. */
    fun post(secrets: MailSecrets, to: String, code: String): Int
}

data class MailSecrets(val apiKey: String, val fromAddress: String) {
    fun configured(): Boolean = acceptableSecret(apiKey) && acceptableFrom(fromAddress)

    companion object {
        const val API_KEY = "FINWISE_RESEND_API_KEY"
        const val FROM = "FINWISE_RESEND_FROM"

        fun load(): MailSecrets = resolve(
            env = { name -> System.getenv(name) },
            fileText = localFileText(),
        )

        fun resolve(env: (String) -> String?, fileText: String?): MailSecrets {
            val file = fileText?.let(::parse).orEmpty()
            return MailSecrets(pick(API_KEY, env, file), pick(FROM, env, file))
        }

        fun parse(text: String): Map<String, String> {
            val out = linkedMapOf<String, String>()
            for (raw in text.lineSequence()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue
                val eq = line.indexOf('=')
                if (eq <= 0) continue
                val name = line.substring(0, eq).trim()
                var value = line.substring(eq + 1).trim()
                if (value.length >= 2 &&
                    ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'")))
                ) {
                    value = value.substring(1, value.length - 1).trim()
                }
                if (name.isNotEmpty()) out[name] = value
            }
            return out
        }
    }
}

private fun pick(name: String, env: (String) -> String?, file: Map<String, String>): String {
    val fromEnv = env(name)?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv
    return file[name].orEmpty().trim()
}

private fun localFileText(): String? {
    val pointed = System.getenv("FINWISE_EMAIL_LOCAL_PROPERTIES")?.trim().orEmpty()
    val paths = listOfNotNull(
        pointed.takeIf { it.isNotEmpty() },
        "email.local.properties",
        "../email.local.properties",
    )
    for (path in paths) {
        val file = java.io.File(path)
        if (file.isFile) return file.readText()
    }
    return null
}

private fun acceptableSecret(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.isNotEmpty() && trimmed.length <= 200 && trimmed.none { it.isWhitespace() }
}

private fun acceptableFrom(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.isNotEmpty() && trimmed.length <= 200 && trimmed.none { it == '\n' || it == '\r' }
}

private const val DEBUG_CODE = "000000"

private val emailPattern = Regex("""^[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,}$""")

internal fun normalizeEmail(raw: String): String? {
    val value = raw.trim().lowercase()
    if (value.length > 120 || value.any { it.isWhitespace() }) return null
    if (!emailPattern.matches(value)) return null
    return value
}

internal fun sixDigitCode(nextInt: (Int) -> Int = { bound -> kotlin.random.Random.nextInt(bound) }): String {
    val n = nextInt(1_000_000)
    return (if (n == 0) 1 else n).toString().padStart(6, '0')
}

internal fun jsonStringField(json: String, name: String): String? {
    val key = "\"$name\""
    val idx = json.indexOf(key)
    if (idx < 0) return null
    var i = idx + key.length
    while (i < json.length && json[i].isWhitespace()) i++
    if (i >= json.length || json[i] != ':') return null
    i++
    while (i < json.length && json[i].isWhitespace()) i++
    if (i >= json.length || json[i] != '"') return null
    i++
    val out = StringBuilder()
    while (i < json.length) {
        val ch = json[i]
        if (ch == '\\') {
            if (i + 1 >= json.length) return null
            out.append(json[i + 1])
            i += 2
            continue
        }
        if (ch == '"') return out.toString()
        out.append(ch)
        i++
    }
    return null
}

internal fun resendJson(from: String, to: String, code: String): String {
    val text = "Your Finwise sign-in code is $code.\n\nIf you didn't ask for this, you can ignore this email."
    return buildString {
        append("{\"from\":")
        append(jsonQuoted(from))
        append(",\"to\":[")
        append(jsonQuoted(to))
        append("],\"subject\":")
        append(jsonQuoted("Your Finwise code"))
        append(",\"text\":")
        append(jsonQuoted(text))
        append("}")
    }
}

private fun jsonQuoted(value: String): String = buildString {
    append('"')
    for (ch in value) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(ch)
        }
    }
    append('"')
}
