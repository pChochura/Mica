package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that represents a type. It consists of a type name surrounded
 * by brackets (if provided to annotate an array).
 *
 * Examples:
 *  - `int`
 *  - `[string]`
 *  - `[[real]]`
 */
internal sealed class TypeExpression(startingToken: Token) : Expression(startingToken)

internal class ArrayTypeExpression(
    val openBracketToken: Token.SquareBracketOpen,
    val closeBracketToken: Token.SquareBracketClose,
    val typeExpression: TypeExpression,
) : TypeExpression(openBracketToken)

internal class SymbolTypeExpression(
    val symbolToken: Token.Symbol,
) : TypeExpression(symbolToken)
