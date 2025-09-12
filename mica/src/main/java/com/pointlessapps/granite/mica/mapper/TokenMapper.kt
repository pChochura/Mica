package com.pointlessapps.granite.mica.mapper

import com.pointlessapps.granite.mica.lexer.BinaryNumberRule
import com.pointlessapps.granite.mica.lexer.CharRule
import com.pointlessapps.granite.mica.lexer.CommentRule
import com.pointlessapps.granite.mica.lexer.DelimiterRule
import com.pointlessapps.granite.mica.lexer.EOLRule
import com.pointlessapps.granite.mica.lexer.ExponentNumberRule
import com.pointlessapps.granite.mica.lexer.HexNumberRule
import com.pointlessapps.granite.mica.lexer.IntNumberRule
import com.pointlessapps.granite.mica.lexer.InterpolatedStringEnd
import com.pointlessapps.granite.mica.lexer.InterpolatedStringQuote
import com.pointlessapps.granite.mica.lexer.InterpolatedStringStart
import com.pointlessapps.granite.mica.lexer.InvalidRule
import com.pointlessapps.granite.mica.lexer.RealNumberRule
import com.pointlessapps.granite.mica.lexer.StringRule
import com.pointlessapps.granite.mica.lexer.SymbolRule
import com.pointlessapps.granite.mica.lexer.TokenRule
import com.pointlessapps.granite.mica.lexer.WhitespaceRule
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.Token

internal fun TokenRule.Match.toToken(): Token = when (token) {
    SymbolRule -> when (value) {
        Keyword.TRUE.value, Keyword.FALSE.value -> Token.BooleanLiteral(location, value)
        in Keyword.valuesLiteral -> Token.Keyword(location, value)
        else -> Token.Symbol(location, value)
    }

    DelimiterRule -> value.toDelimiterToken(location)
    RealNumberRule -> Token.NumberLiteral(location, value, Token.NumberLiteral.Type.Real)
    IntNumberRule -> Token.NumberLiteral(location, value, Token.NumberLiteral.Type.Int)
    HexNumberRule -> Token.NumberLiteral(location, value, Token.NumberLiteral.Type.Hex)
    BinaryNumberRule -> Token.NumberLiteral(location, value, Token.NumberLiteral.Type.Binary)
    ExponentNumberRule -> Token.NumberLiteral(location, value, Token.NumberLiteral.Type.Exponent)
    CharRule -> Token.CharLiteral(location, value.escape()[1]) // Strip the quotes
    StringRule -> Token.StringLiteral(location, value.escape())
    InterpolatedStringQuote -> Token.InterpolatedStringQuote(location)
    InterpolatedStringStart -> Token.InterpolatedStringStart(location)
    InterpolatedStringEnd -> Token.InterpolatedStringEnd(location)
    CommentRule -> Token.Comment(location, value)
    EOLRule -> Token.EOL(location)
    WhitespaceRule -> Token.Whitespace(location, value)
    InvalidRule -> Token.Invalid(location, value)
}

private fun String.toDelimiterToken(location: Location): Token = when (this) {
    ":" -> Token.Colon(location)
    "," -> Token.Comma(location)
    "@" -> Token.At(location)
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
    "." -> Token.Dot(location)
    "=" -> Token.Equals(location)
    "+=" -> Token.PlusEquals(location)
    "-=" -> Token.MinusEquals(location)
    "++" -> Token.Increment(location)
    "--" -> Token.Decrement(location)
    else -> Token.Invalid(location, this)
}
