package com.pointlessapps.granite.mica.mapper

internal fun String.escape(): String = replace(Regex("\\\\.")) {
    when (it.value) {
        "\\n" -> "\n"
        "\\r" -> "\r"
        "\\t" -> "\t"
        "\\'" -> "'"
        "\\\"" -> "\""
        else -> it.value
    }
}
