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

    private sealed interface State {
        object Normal : State
        object String : State
        class Interpolation(var bracketBalanceCount: Int) : State
    }

    private val stateStack = mutableListOf<State>(State.Normal)
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

        if (state == State.Normal) {
            if (isQuote(currentIndex)) {
                stateStack.add(State.String)
                matchedToken = parseQuote()
            } else {
                matchedToken = parseNormal()
            }
        } else if (state == State.String) {
            matchedToken = parseString()
        } else if (state is State.Interpolation) {
            if (isQuote(currentIndex)) {
                stateStack.add(State.String)
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
        token.matchingStrategy.match(input, currentIndex)?.let {
            val location = Location(
                line = currentLine,
                column = currentColumn,
                // We only match at the beginning of the string so we can just use the index
                length = it.length,
            )

            TokenRule.Match(location, token, it)
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
            stateStack.add(State.Interpolation(1))
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
            (state as State.Interpolation).bracketBalanceCount++
        } else if (input[currentIndex] == ')') {
            if (--(state as State.Interpolation).bracketBalanceCount == 0) {
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
