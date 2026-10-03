package com.aicfo.shared.auth

/**
 * Optional profile phone. US display helpers only.
 * The number is not an account session and is not required to enter the app.
 */
object PhoneNumbers {
    fun usDigits(raw: String): String = raw.filter { it.isDigit() }.take(10)

    fun toE164(nationalDigits: String): String = "+1${usDigits(nationalDigits)}"

    /** NANP: 10 digits, area code and exchange cannot start with 0 or 1. */
    fun isValidUs(e164: String): Boolean = e164.matches(Regex("""\+1[2-9]\d{2}[2-9]\d{6}"""))

    fun formatNational(nationalDigits: String): String {
        val d = usDigits(nationalDigits)
        if (d.isEmpty()) return ""
        val area = d.take(3)
        val prefix = d.drop(3).take(3)
        val line = d.drop(6).take(4)
        return buildString {
            append("(")
            append(area)
            if (d.length >= 3) append(") ")
            append(prefix)
            if (d.length >= 6) append("-")
            append(line)
        }
    }

    /** Profile line: +1····1234 */
    fun maskTight(e164: String): String = "+1····${e164.takeLast(4)}"

    /** Settings line: +1 ····1234 */
    fun maskSpaced(e164: String): String = "+1 ····${e164.takeLast(4)}"
}
