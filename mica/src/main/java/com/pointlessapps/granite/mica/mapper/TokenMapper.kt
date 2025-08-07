package com.pointlessapps.granite.mica.mapper

import com.pointlessapps.granite.mica.lexer.BinaryNumber
import com.pointlessapps.granite.mica.lexer.Char
import com.pointlessapps.granite.mica.lexer.Comment
import com.pointlessapps.granite.mica.lexer.Delimiter
import com.pointlessapps.granite.mica.lexer.EOL
import com.pointlessapps.granite.mica.lexer.ExponentNumber
import com.pointlessapps.granite.mica.lexer.GrammarToken
import com.pointlessapps.granite.mica.lexer.HexNumber
import com.pointlessapps.granite.mica.lexer.Invalid
import com.pointlessapps.granite.mica.lexer.Number
import com.pointlessapps.granite.mica.lexer.String
import com.pointlessapps.granite.mica.lexer.Symbol
import com.pointlessapps.granite.mica.lexer.Whitespace
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.Token

internal fun GrammarToken.Match.toToken(): Token = when (token) {
    Symbol -> when (value) {
        Keyword.TRUE.value, Keyword.FALSE.value -> Token.BooleanLiteral(location, value)
        in Keyword.valuesLiteral() -> Token.Keyword(location, value)
        else -> Token.Symbol(location, value)
    }

    Delimiter -> value.toDelimiterToken(location)
    Number -> Token.NumberLiteral(location, value, Token.NumberLiteral.Type.Decimal)
    HexNumber -> Token.NumberLiteral(location, value, Token.NumberLiteral.Type.Hex)
    BinaryNumber -> Token.NumberLiteral(location, value, Token.NumberLiteral.Type.Binary)
    ExponentNumber -> Token.NumberLiteral(location, value, Token.NumberLiteral.Type.Exponent)
    Char -> Token.CharLiteral(location, value[1]) // Strip the quotes
    String -> Token.StringLiteral(location, value.trim('\"')) // Strip the quotes
    Comment -> Token.Comment(location, value)
    EOL -> Token.EOL(location)
    Whitespace -> Token.Whitespace(location, value)
    Invalid -> Token.Invalid(location, value)
    else -> Token.Invalid(location, value)
}

private fun String.toDelimiterToken(location: Location): Token = when (this) {
    "$" -> Token.Dollar(location)
    ":" -> Token.Colon(location)
    "," -> Token.Comma(location)
    "(" -> Token.BracketOpen(location)
    ")" -> Token.BracketClose(location)
    "{" -> Token.CurlyBracketOpen(location)
    "}" -> Token.CurlyBracketClose(location)
    "[" -> Token.SquareBracketOpen(location)
    "]" -> Token.SquareBracketClose(location)
    "-" -> Token.Operator(location, Token.Operator.Type.Subtract)
    "+" -> Token.Operator(location, Token.Operator.Type.Add)
    "^" -> Token.Operator(location, Token.Operator.Type.Exponent)
    "/" -> Token.Operator(location, Token.Operator.Type.Divide)
    "*" -> Token.Operator(location, Token.Operator.Type.Multiply)
    "|" -> Token.Operator(location, Token.Operator.Type.Or)
    "&" -> Token.Operator(location, Token.Operator.Type.And)
    "!" -> Token.Operator(location, Token.Operator.Type.Not)
    "==" -> Token.Operator(location, Token.Operator.Type.Equals)
    "!=" -> Token.Operator(location, Token.Operator.Type.NotEquals)
    ">" -> Token.Operator(location, Token.Operator.Type.GraterThan)
    "<" -> Token.Operator(location, Token.Operator.Type.LessThan)
    ">=" -> Token.Operator(location, Token.Operator.Type.GraterThanOrEquals)
    "<=" -> Token.Operator(location, Token.Operator.Type.LessThanOrEquals)
    ".." -> Token.Operator(location, Token.Operator.Type.Range)
    "=" -> Token.Equals(location)
    else -> Token.Invalid(location, this)
}
