package com.pointlessapps.granite.mica.mapper

import com.pointlessapps.granite.mica.model.BinaryNumber
import com.pointlessapps.granite.mica.model.Boolean
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
    Symbol -> {
        if (result.value in Keywords) Token.Keyword(result.value)
        else Token.Symbol(result.value)
    }

    Delimiter -> {
        when (result.value) {
            ">" -> Token.GraterThan
            "<" -> Token.LessThan
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
            "==" -> Token.Operator(Token.Operator.Type.Compare)
            ".." -> Token.Operator(Token.Operator.Type.Range)
            "=" -> Token.Equals
            else -> Token.Invalid(result.value)
        }
    }

    Number -> Token.NumberLiteral(result.value, Token.NumberLiteral.Type.Decimal)
    HexNumber -> Token.NumberLiteral(result.value, Token.NumberLiteral.Type.Hex)
    BinaryNumber -> Token.NumberLiteral(result.value, Token.NumberLiteral.Type.Binary)
    ExponentNumber -> Token.NumberLiteral(result.value, Token.NumberLiteral.Type.Exponent)
    String -> Token.StringLiteral(result.value)
    Boolean -> Token.BooleanLiteral(result.value)
    Comment -> Token.Comment(result.value)
    Whitespace -> {
        if (result.value == "\n") Token.EOL
        else Token.Whitespace(result.value)
    }

    else -> null
}
