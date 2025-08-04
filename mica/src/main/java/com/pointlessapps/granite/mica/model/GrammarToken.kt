package com.pointlessapps.granite.mica.model

internal class Grammar(private val input: String) {

    // The order of the tokens is important, as the longest token will be matched first
    private val tokens: Sequence<GrammarToken> = sequenceOf(
        Comment,
        String,
        ExponentNumber,
        BinaryNumber,
        HexNumber,
        Number,
        Symbol,
        Delimiter,
        EOL,
        Whitespace,
    )

    private var currentIndex = 0
    private var currentLine = 0
    private var currentColumn = 0

    fun tokenizeNext(): GrammarToken.Match? {
        if (currentIndex >= input.length) {
            return null
        }

        val matchedToken = tokens.map { token ->
            token.regex.find(input.substring(currentIndex))?.let {
                val location = Location(
                    line = currentLine,
                    column = currentColumn,
                    // We only match at the beginning of the string so we can just use the index
                    length = it.range.last + 1,
                )

                GrammarToken.Match(location, token, it.value)
            }
        }.firstOrNull { it != null } ?: GrammarToken.Match(
            location = Location(
                line = currentLine,
                column = currentColumn,
                length = 1,
            ),
            token = Invalid,
            value = input[currentIndex].toString(),
        )

        currentColumn += matchedToken.location.length
        currentIndex += matchedToken.location.length

        if (matchedToken.token == EOL) {
            currentLine++
            currentColumn = 0
        }

        return matchedToken
    }
}

internal data class GrammarToken(val regex: Regex) {
    data class Match(
        val location: Location,
        val token: GrammarToken,
        val value: String,
    )
}

internal val Symbol = GrammarToken(Regex("\\A[a-zA-Z_][a-zA-Z0-9_]*"))
internal val Delimiter = GrammarToken(Regex("\\A\\.\\.|\\A==|\\A!=|\\A>=|\\A<=|\\A[!<>(){}:,\\-+|&=*/$^\\[\\]]"))
internal val Number = GrammarToken(Regex("\\A[0-9][0-9_]*(?:\\.[0-9_]+)?"))
internal val HexNumber = GrammarToken(Regex("\\A0x[0-9]+"))
internal val BinaryNumber = GrammarToken(Regex("\\A0b[0-1]+"))
internal val ExponentNumber = GrammarToken(Regex("\\A[0-9][0-9_]*e-?[0-9][0-9_]*"))
internal val String = GrammarToken(Regex("\\A\".*?\"")) // TODO interpolated string
internal val Comment = GrammarToken(Regex("\\A//.*")) // TODO multiline comment
internal val Whitespace = GrammarToken(Regex("\\s+"))
internal val EOL = GrammarToken(Regex("\\A\n"))

internal val Invalid = GrammarToken(Regex("."))
