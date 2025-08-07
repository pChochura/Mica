package com.pointlessapps.granite.mica.lexer

import com.pointlessapps.granite.mica.mapper.toToken
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.Token

/**
 * Converts the input into a sequence of tokens.
 * Skips the whitespaces and comments.
 */
class Lexer(private val input: String) {

    // The order of the tokens is important, as the longest token will be matched first
    private val tokens: Sequence<GrammarToken> = sequenceOf(
        Comment,
        Char,
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

    fun tokenizeNext(): Token {
        var token: Token
        do {
            token = matchToken()?.toToken() ?: Token.EOF()
        } while (token is Token.Whitespace || token is Token.Comment)

        return token
    }

    // FIXME whitespace at the end of a statement
    private fun matchToken(): GrammarToken.Match? {
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
