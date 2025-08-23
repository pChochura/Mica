package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.lexer.Lexer
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.statement.parseListOfStatements

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
    inline fun <reified T : Token> expectToken(
        currentlyParsing: String,
        condition: (T) -> Boolean = { true },
    ): T {
        val token = getToken()
        assert(token is T && condition(token)) {
            throw UnexpectedTokenException(T::class.simpleName.orEmpty(), token, currentlyParsing)
        }

        advance()

        return token as T
    }

    inline fun <reified T : Token> skipTokens() {
        while (isToken<T>()) advance()
    }

    fun expectEOForEOL(currentlyParsing: String) {
        val token = getToken()
        assert(token.let { it is Token.EOF || it is Token.EOL }) {
            throw UnexpectedTokenException("EOF or EOL", token, currentlyParsing)
        }

        advance()
    }

    fun parse(): Root {
        if (getToken() is Token.EOF) return Root(emptyList())
        val statements = parseListOfStatements { it !is Token.EOF }
        return Root(statements)
    }
}
