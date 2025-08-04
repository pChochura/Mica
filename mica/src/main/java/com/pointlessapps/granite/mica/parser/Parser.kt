package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.Lexer
import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token

data class Parser(private val lexer: Lexer) {

    private val tokens = mutableListOf<Token>()
    var currentIndex: Int = 0
        private set

    fun restoreTo(index: Int) {
        currentIndex = index
    }

    fun advance() = currentIndex++
    fun getToken(): Token {
        while (currentIndex >= tokens.size) {
            tokens.add(lexer.tokenizeNext())
        }

        return tokens[currentIndex]
    }

    inline fun <reified T : Token> expectToken(condition: (T) -> Boolean = { true }): T {
        val token = getToken()
        assert(T::class.isInstance(token) && condition(token as T)) {
            throw UnexpectedTokenException("Expected ${T::class.simpleName}, but got $token")
        }

        advance()

        return token as T
    }

    fun expectEOForEOL() {
        val token = getToken()
        assert(token.let { it == Token.EOF || it == Token.EOL }) {
            throw UnexpectedTokenException(
                "Expected ${Token.EOF} or ${Token.EOL}, but got $token",
            )
        }

        advance()
    }

    fun <T> parseInSequence(vararg elements: () -> T): T? {
        elements.forEach {
            val savedIndex = currentIndex
            val element = runCatching { it.invoke() }.getOrNull()
            if (element != null) {
                return element
            }

            restoreTo(savedIndex)
        }

        return null
    }

    fun parse(): Root {
        if (getToken() == Token.EOF) return Root(emptyList())
        val statements = parseListOfStatements(parseUntilCondition = { it != Token.EOF })
        return Root(statements)
    }
}