package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token

data class Parser(
    private val tokens: List<Token>,
) {
    private var savedIndex: Int = 0
    private var currentIndex: Int = 0

    fun save() {
        savedIndex = currentIndex
    }

    fun restore() {
        currentIndex = savedIndex
    }

    fun advance() = currentIndex++
    fun getToken(): Token = tokens.getOrNull(currentIndex) ?: Token.EOF

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
            save()
            val element = runCatching { it.invoke() }.getOrNull()
            if (element != null) {
                return element
            }

            restore()
        }

        return null
    }

    fun parse(): Root {
        if (getToken() == Token.EOF) return Root(emptyList())
        val statements = parseListOfStatements(parseUntilCondition = { it != Token.EOF })
        return Root(statements)
    }
}