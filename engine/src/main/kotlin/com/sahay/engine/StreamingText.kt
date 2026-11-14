package com.sahay.engine

/** Decode only a text field from an incomplete JSON reply. Provisional text never becomes learner state. */
class StreamingText(field: String) {
    private val start = Regex("\"${Regex.escape(field)}\"\\s*:\\s*\"")
    private val raw = StringBuilder()
    private var previous = ""
    fun accept(token: String): String? {
        require(raw.length + token.length <= 128_000) { "reply exceeded the streaming limit" }
        raw.append(token)
        val match = start.find(raw) ?: return null
        val text = StringBuilder()
        var i = match.range.last + 1
        while (i < raw.length) {
            val c = raw[i++]
            if (c == '"') break
            if (c != '\\') { if (c < ' ') break; text.append(c); continue }
            if (i == raw.length) break
            when (val escaped = raw[i++]) {
                '"', '\\', '/' -> text.append(escaped)
                'n' -> text.append('\n')
                'r' -> text.append('\r')
                't' -> text.append('\t')
                'b' -> text.append('\b')
                'f' -> text.append('\u000c')
                'u' -> {
                    if (i + 4 > raw.length) break
                    val code = raw.substring(i, i + 4).toIntOrNull(16) ?: break
                    text.append(code.toChar()); i += 4
                }
                else -> break
            }
        }
        val value = text.toString()
        if (value == previous) return null
        previous = value
        return value
    }
}
