package com.aicfo.app.i18n

import android.content.Context
import com.aicfo.shared.market.LocalStrings

/**
 * Reads `res/values/strings.xml` (and locale overlays) by catalog key.
 * `reg.never_move` looks up `reg_never_move`. Null means the shared catalog wins.
 */
class AndroidLocalStrings(private val context: Context) : LocalStrings {
    override fun text(key: String): String? {
        val name = key.replace('.', '_')
        val id = context.resources.getIdentifier(name, "string", context.packageName)
        if (id == 0) return null
        return context.getString(id)
    }
}
