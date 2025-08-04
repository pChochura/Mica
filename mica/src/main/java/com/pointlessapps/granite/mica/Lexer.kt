package com.pointlessapps.granite.mica

import com.pointlessapps.granite.mica.mapper.toToken
import com.pointlessapps.granite.mica.model.Grammar
import com.pointlessapps.granite.mica.model.Token

class Lexer(private val input: String) {

    private val grammar: Grammar = Grammar()
    private var index: Int = 0

    fun tokenizeNext(): Token {
        var token: Token
        do {
            if (index >= input.length) {
                return Token.EOF
            }

            val match = grammar.parse(input, index)
            token = match.toToken() ?: Token.Invalid(input[index].toString())
            if (match != null) {
                index += match.result.range.last + 1
            } else {
                index++
            }
        } while (token is Token.Whitespace || token is Token.Comment)

        return token
    }
}
