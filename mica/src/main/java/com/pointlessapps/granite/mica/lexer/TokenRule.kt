package com.pointlessapps.granite.mica.lexer

import com.pointlessapps.granite.mica.model.Location

// TODO rethink the logic (maybe dont use the regex for all cases?)
internal data class TokenRule(val regex: Regex) {
    data class Match(
        val location: Location,
        val token: TokenRule,
        val value: String,
    )
}

internal val Symbol = TokenRule(Regex("\\A[a-zA-Z_][a-zA-Z0-9_]*"))
internal val Delimiter = TokenRule(Regex("\\A\\.\\.|\\A==|\\A!=|\\A>=|\\A<=|\\A[!<>(){}:,\\-+|&=*/$^\\[\\]]"))
internal val IntNumber = TokenRule(Regex("\\A[0-9][0-9_]*"))
internal val RealNumber = TokenRule(Regex("\\A[0-9][0-9_]*\\.[0-9][0-9_]*"))
internal val HexNumber = TokenRule(Regex("\\A0x[0-9a-fA-F]+"))
internal val BinaryNumber = TokenRule(Regex("\\A0b[0-1]+"))
internal val ExponentNumber = TokenRule(Regex("\\A[0-9][0-9_]*(?:\\.[0-9][0-9_]*)?e-?[0-9][0-9_]*"))
internal val Char = TokenRule(Regex("\\A'.'"))
internal val String = TokenRule(Regex("\\A\".*?\"")) // TODO interpolated string
internal val Comment = TokenRule(Regex("\\A//[^\n]*|\\A/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL))
internal val Whitespace = TokenRule(Regex("\\A[ \\t]+"))
internal val EOL = TokenRule(Regex("\\A\n"))

internal val Invalid = TokenRule(Regex("."))
