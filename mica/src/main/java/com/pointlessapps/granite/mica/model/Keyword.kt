package com.pointlessapps.granite.mica.model

internal enum class Keyword(val value: String) {
    NUMBER("number"),
    BOOL("bool"),
    STRING("string"),
    TRUE("true"),
    FALSE("false"),
    RETURN("return"),
    IF("if"),
    ELSE("else"),
    LOOP("loop");

    companion object {
        val valuesLiteral = entries.map(Keyword::value)
    }
}
