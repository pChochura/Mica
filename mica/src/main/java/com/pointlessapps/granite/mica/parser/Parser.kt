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

    inline fun <reified T : Token> isToken() = getToken() is T
    inline fun <reified T : Token> expectToken(condition: (T) -> Boolean = { true }): T {
        val token = getToken()
        assert(T::class.isInstance(token) && condition(token as T)) {
            throw UnexpectedTokenException(T::class.simpleName.orEmpty(), token)
        }

        advance()

        return token as T
    }

    fun expectEOForEOL() {
        val token = getToken()
        assert(token.let { it is Token.EOF || it is Token.EOL }) {
            throw UnexpectedTokenException("EOF or EOL", token)
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
        if (getToken() is Token.EOF) return Root(emptyList())
        val statements = parseListOfStatements(parseUntilCondition = { it !is Token.EOF })
        return Root(statements)
    }
}