package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * A sequence of tokens that after the evaluation produces a value.
 */
internal sealed class Expression(val startingToken: Token)

internal object EmptyExpression : Expression(Token.EOF())

/**
 * Expression that consists of a character enclosed by single quotes.
 */
internal class CharLiteralExpression(val token: Token.CharLiteral) : Expression(token)

/**
 * Expression that consists of a string enclosed by double quotes.
 */
internal class StringLiteralExpression(val token: Token.StringLiteral) : Expression(token)

/**
 * Expression that consists of a number.
 *
 * Examples:
 *  - `123`
 *  - `100_100`
 *  - `3.14`
 *  - `10e5`
 *  - `5e-31`
 *  - `0x123`
 *  - `0b1010`
 */
internal class NumberLiteralExpression(val token: Token.NumberLiteral) : Expression(token)

/**
 * Expression that consists of a boolean value (true or false)
 */
internal class BooleanLiteralExpression(val token: Token.BooleanLiteral) : Expression(token)

/**
 * Expression that consists of a alphanumeric symbol.
 *
 * Examples:
 *  - `a`
 *  - `abc`
 *  - `a_123`
 */
internal class SymbolExpression(val token: Token.Symbol) : Expression(token)
