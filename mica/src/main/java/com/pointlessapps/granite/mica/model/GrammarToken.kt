package com.pointlessapps.granite.mica.model

internal class Grammar {
    val tokens: Sequence<GrammarToken> = sequenceOf(
        Comment,
        String,
        ExponentNumber,
        BinaryNumber,
        HexNumber,
        Number,
        Boolean,
        Symbol,
        Delimiter,
        Whitespace,
    )

    fun parse(input: String, startIndex: Int) = tokens.map { token ->
        token.regex.find(input.substring(startIndex))
            ?.let { GrammarToken.Match(token, it) }
    }.firstOrNull { it != null }
}

internal data class GrammarToken(val regex: Regex) {
    data class Match(
        val token: GrammarToken,
        val result: MatchResult,
    )
}

internal val Symbol = GrammarToken(Regex("\\A[a-zA-Z_][a-zA-Z0-9_]*"))
internal val Delimiter = GrammarToken(Regex("\\A\\.\\.|\\A==|\\A!=|\\A>=|\\A<=|\\A[!<>(){}:,\\-+|&=*/$^\\[\\]]"))
internal val Number = GrammarToken(Regex("\\A[0-9][0-9_]*(?:\\.[0-9_]+)?"))
internal val HexNumber = GrammarToken(Regex("\\A0x[0-9]+"))
internal val BinaryNumber = GrammarToken(Regex("\\A0b[0-1]+"))
internal val ExponentNumber = GrammarToken(Regex("\\A[0-9][0-9_]*e-?[0-9][0-9_]*"))
internal val String = GrammarToken(Regex("\\A\".*?\"")) // TODO interpolated string
internal val Boolean = GrammarToken(Regex("\\Atrue|\\Afalse"))
internal val Comment = GrammarToken(Regex("\\A//.*"))
internal val Whitespace = GrammarToken(Regex("\\A\n|\\s+"))
