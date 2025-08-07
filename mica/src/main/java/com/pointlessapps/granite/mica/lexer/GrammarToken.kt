package com.pointlessapps.granite.mica.lexer

import com.pointlessapps.granite.mica.model.Location

// TODO rethink the logic (maybe dont use the regex for all cases?)
internal data class GrammarToken(val regex: Regex) {
    data class Match(
        val location: Location,
        val token: GrammarToken,
        val value: String,
    )
}

internal val Symbol = GrammarToken(Regex("\\A[a-zA-Z_][a-zA-Z0-9_]*"))
internal val Delimiter = GrammarToken(Regex("\\A\\.\\.|\\A==|\\A!=|\\A>=|\\A<=|\\A[!<>(){}:,\\-+|&=*/$^\\[\\]]"))
internal val Number = GrammarToken(Regex("\\A[0-9][0-9_]*(?:\\.[0-9][0-9_]*)?"))
internal val HexNumber = GrammarToken(Regex("\\A0x[0-9]+"))
internal val BinaryNumber = GrammarToken(Regex("\\A0b[0-1]+"))
internal val ExponentNumber = GrammarToken(Regex("\\A[0-9][0-9_]*(?:\\.[0-9][0-9_]*)?e-?[0-9][0-9_]*"))
internal val Char = GrammarToken(Regex("\\A'.'"))
internal val String = GrammarToken(Regex("\\A\".*?\"")) // TODO interpolated string
internal val Comment = GrammarToken(Regex("\\A//[^\n]*|\\A/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL))
internal val Whitespace = GrammarToken(Regex("\\A\\s+"))
internal val EOL = GrammarToken(Regex("\\A\n"))

internal val Invalid = GrammarToken(Regex("."))
