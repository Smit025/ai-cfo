package com.aicfo.shared.auth

/**
 * Where a release build finds the Resend key.
 *
 * The names are not secret. Values are read from the process environment first,
 * then from the text of gitignored `email.local.properties`. Nothing here has a default key.
 */
object MailConfig {
    const val API_KEY = "FINWISE_RESEND_API_KEY"
    const val FROM = "FINWISE_RESEND_FROM"

    fun resolve(env: (String) -> String?, fileText: String?): Pair<String, String> {
        val file = fileText?.let(::parse).orEmpty()
        return pick(API_KEY, env, file) to pick(FROM, env, file)
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
                value = value.substring(1, value.length - 1)
            }
            if (name.isNotEmpty()) out[name] = value.trim()
        }
        return out
    }

    private fun pick(name: String, env: (String) -> String?, file: Map<String, String>): String {
        val fromEnv = env(name)?.trim().orEmpty()
        if (fromEnv.isNotEmpty()) return fromEnv
        return file[name].orEmpty().trim()
    }
}
