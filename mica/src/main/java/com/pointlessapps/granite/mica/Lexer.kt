package com.pointlessapps.granite.mica

import com.pointlessapps.granite.mica.mapper.toToken
import com.pointlessapps.granite.mica.model.Grammar
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

class Lexer {
    private val grammar: Grammar = Grammar()

    fun parseInput(input: String): Parser {
        val tokens: MutableList<Token> = mutableListOf()
        var index = 0
        while (index < input.length) {
            val match = grammar.parse(input, index)
            val token = match.toToken() ?: Token.Invalid(input[index].toString())
            if (token !is Token.Whitespace && token !is Token.Comment) {
                tokens.add(token)
            }
            if (match != null) {
                index += match.result.range.last + 1
            } else {
                index++
            }
        }

        tokens.add(Token.EOF)

        return Parser(tokens.toList())
    }
}
