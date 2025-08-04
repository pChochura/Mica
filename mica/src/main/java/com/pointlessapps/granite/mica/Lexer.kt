package com.pointlessapps.granite.mica

import com.pointlessapps.granite.mica.mapper.toToken
import com.pointlessapps.granite.mica.model.Grammar
import com.pointlessapps.granite.mica.model.Token

class Lexer(input: String) {

    private val grammar: Grammar = Grammar(input)

    fun tokenizeNext(): Token {
        var token: Token
        do {
            val match = grammar.tokenizeNext()
            token = match?.toToken() ?: Token.EOF()
        } while (token is Token.Whitespace || token is Token.Comment)

        return token
    }
}
