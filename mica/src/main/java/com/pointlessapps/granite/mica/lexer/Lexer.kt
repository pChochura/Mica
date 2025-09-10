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
    private val tokens: Sequence<TokenRule> = sequenceOf(
        CommentRule,
        CharRule,
        StringRule,
        ExponentNumberRule,
        BinaryNumberRule,
        HexNumberRule,
        RealNumberRule,
        IntNumberRule,
        SymbolRule,
        DelimiterRule,
        EOLRule,
        WhitespaceRule,
    )

    private enum class State { NORMAL, STRING, INTERPOLATION }

    private var bracketBalanceCount = 0
    private val stateStack = mutableListOf<State>(State.NORMAL)
    private val state: State
        get() = stateStack.last()

    private var currentIndex = 0
    private var currentLine = 1
    private var currentColumn = 0

    fun tokenizeNext(): Token {
        var token: Token
        do {
            token = matchToken()?.toToken() ?: Token.EOF(
                Location(
                    line = currentLine,
                    column = currentColumn,
                    length = 1,
                ),
            )
        } while (token is Token.Whitespace || token is Token.Comment)

        return token
    }

    private fun matchToken(): TokenRule.Match? {
        if (currentIndex >= input.length) {
            return null
        }

        var matchedToken: TokenRule.Match? = null

        if (state == State.NORMAL) {
            if (isQuote(currentIndex)) {
                stateStack.add(State.STRING)
                matchedToken = parseQuote()
            } else {
                matchedToken = parseNormal()
            }
        } else if (state == State.STRING) {
            matchedToken = parseString()
        } else if (state == State.INTERPOLATION) {
            if (isQuote(currentIndex)) {
                stateStack.add(State.STRING)
                matchedToken = parseQuote()
            } else {
                matchedToken = parseInterpolatedString()
            }
        }

        if (matchedToken == null) return null

        currentColumn += matchedToken.location.length
        currentIndex += matchedToken.location.length

        if (matchedToken.token == EOLRule) {
            currentLine++
            currentColumn = 0
        }

        return matchedToken
    }

    private fun parseNormal(): TokenRule.Match = tokens.map { token ->
        token.regex.find(input.substring(currentIndex))?.let {
            val location = Location(
                line = currentLine,
                column = currentColumn,
                // We only match at the beginning of the string so we can just use the index
                length = it.range.last + 1,
            )

            TokenRule.Match(location, token, it.value)
        }
    }.firstOrNull { it != null } ?: TokenRule.Match(
        location = Location(
            line = currentLine,
            column = currentColumn,
            length = 1,
        ),
        token = InvalidRule,
        value = input[currentIndex].toString(),
    )

    private fun parseString(): TokenRule.Match? {
        if (isQuote(currentIndex)) {
            stateStack.removeLastOrNull()
            return parseQuote()
        } else if (isInterpolationStart(currentIndex)) {
            stateStack.add(State.INTERPOLATION)
            return parseInterpolationStart()
        }

        val startingPosition = currentIndex
        var index = currentIndex
        while (index < input.length) {
            if (isQuote(index) || isInterpolationStart(index)) {
                val length = index - startingPosition

                return TokenRule.Match(
                    location = Location(
                        line = currentLine,
                        column = currentColumn + length,
                        length = length,
                    ),
                    token = StringRule,
                    value = input.substring(startingPosition, index),
                )
            }

            index++
        }

        return null
    }

    private fun parseInterpolatedString(): TokenRule.Match {
        if (input[currentIndex] == '(') {
            bracketBalanceCount++
        } else if (input[currentIndex] == ')') {
            if (bracketBalanceCount-- == 0) {
                stateStack.removeLastOrNull()

                return parseInterpolationEnd()
            }
        }

        return parseNormal()
    }

    private fun parseQuote(): TokenRule.Match = TokenRule.Match(
        location = Location(
            line = currentLine,
            column = currentColumn,
            length = 1,
        ),
        token = InterpolatedStringQuote,
        value = input[currentIndex].toString(),
    )

    private fun parseInterpolationStart(): TokenRule.Match = TokenRule.Match(
        location = Location(
            line = currentLine,
            column = currentColumn,
            length = 2,
        ),
        token = InterpolatedStringStart,
        value = input.substring(currentIndex, currentIndex + 2),
    )

    private fun parseInterpolationEnd(): TokenRule.Match = TokenRule.Match(
        location = Location(
            line = currentLine,
            column = currentColumn,
            length = 1,
        ),
        token = InterpolatedStringEnd,
        value = input[currentIndex].toString(),
    )

    private fun isQuote(index: Int) = input[index] == '"' && input.getOrNull(index - 1) != '\\'
    private fun isInterpolationStart(index: Int) = input.startsWith("$(", startIndex = index) &&
            input.getOrNull(index - 1) != '\\'
}
