package com.pointlessapps.granite.mica.mapper

import com.pointlessapps.granite.mica.model.BinaryNumber
import com.pointlessapps.granite.mica.model.Comment
import com.pointlessapps.granite.mica.model.Delimiter
import com.pointlessapps.granite.mica.model.ExponentNumber
import com.pointlessapps.granite.mica.model.GrammarToken
import com.pointlessapps.granite.mica.model.HexNumber
import com.pointlessapps.granite.mica.model.Keywords
import com.pointlessapps.granite.mica.model.Number
import com.pointlessapps.granite.mica.model.String
import com.pointlessapps.granite.mica.model.Symbol
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Whitespace

internal fun GrammarToken.Match?.toToken(): Token? = when (this?.token) {
    Symbol -> when (result.value) {
        "true", "false" -> Token.BooleanLiteral(result.value)
        in Keywords -> Token.Keyword(result.value)
        else -> Token.Symbol(result.value)
    }

    Delimiter -> result.value.toDelimiterToken()
    Number -> Token.NumberLiteral(result.value, Token.NumberLiteral.Type.Decimal)
    HexNumber -> Token.NumberLiteral(result.value, Token.NumberLiteral.Type.Hex)
    BinaryNumber -> Token.NumberLiteral(result.value, Token.NumberLiteral.Type.Binary)
    ExponentNumber -> Token.NumberLiteral(result.value, Token.NumberLiteral.Type.Exponent)
    String -> Token.StringLiteral(result.value)
    Comment -> Token.Comment(result.value)
    Whitespace -> when (result.value) {
        "\n" -> Token.EOL
        else -> Token.Whitespace(result.value)
    }

    else -> null
}

private fun String.toDelimiterToken(): Token = when (this) {
    "$" -> Token.Dollar
    ":" -> Token.Colon
    "," -> Token.Comma
    "(" -> Token.BracketOpen
    ")" -> Token.BracketClose
    "{" -> Token.CurlyBracketOpen
    "}" -> Token.CurlyBracketClose
    "[" -> Token.SquareBracketOpen
    "]" -> Token.SquareBracketClose
    "-" -> Token.Operator(Token.Operator.Type.Subtract)
    "+" -> Token.Operator(Token.Operator.Type.Add)
    "^" -> Token.Operator(Token.Operator.Type.Exponent)
    "/" -> Token.Operator(Token.Operator.Type.Divide)
    "*" -> Token.Operator(Token.Operator.Type.Multiply)
    "|" -> Token.Operator(Token.Operator.Type.Or)
    "&" -> Token.Operator(Token.Operator.Type.And)
    "!" -> Token.Operator(Token.Operator.Type.Not)
    "==" -> Token.Operator(Token.Operator.Type.Equals)
    "!=" -> Token.Operator(Token.Operator.Type.NotEquals)
    ">" -> Token.Operator(Token.Operator.Type.GraterThan)
    "<" -> Token.Operator(Token.Operator.Type.LessThan)
    ">=" -> Token.Operator(Token.Operator.Type.GraterThanOrEquals)
    "<=" -> Token.Operator(Token.Operator.Type.LessThanOrEquals)
    ".." -> Token.Operator(Token.Operator.Type.Range)
    "=" -> Token.Equals
    else -> Token.Invalid(this)
}
