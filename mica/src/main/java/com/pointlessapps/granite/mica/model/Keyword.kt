package com.pointlessapps.granite.mica.model

internal enum class Keyword(val value: String) {
    BOOL("bool"),
    INT("int"),
    REAL("real"),
    CHAR("char"),
    STRING("string"),
    INT_RANGE("intRange"),
    REAL_RANGE("realRange"),
    CHAR_RANGE("charRange"),
    TRUE("true"),
    FALSE("false"),
    RETURN("return"),
    IF("if"),
    ELSE("else"),
    LOOP("loop"),
    BREAK("break");

    companion object {
        val valuesLiteral = entries.map(Keyword::value)
    }
}
