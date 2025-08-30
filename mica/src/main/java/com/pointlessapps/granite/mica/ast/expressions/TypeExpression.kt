package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that represents a type. It consists of a type name surrounded
 * by square brackets (if provided to annotate an array)
 * or curly brackets (if provided to annotate a set).
 *
 * Examples:
 *  - `int`
 *  - `[string]`
 *  - `[[real]]`
 *  - `{int}`
 *  - `{{string}}`
 */
internal sealed class TypeExpression(startingToken: Token) : Expression(startingToken)

internal class ArrayTypeExpression(
    val openBracketToken: Token.SquareBracketOpen,
    val closeBracketToken: Token.SquareBracketClose,
    val typeExpression: TypeExpression,
) : TypeExpression(openBracketToken)

internal class SetTypeExpression(
    val openBracketToken: Token.CurlyBracketOpen,
    val closeBracketToken: Token.CurlyBracketClose,
    val typeExpression: TypeExpression,
) : TypeExpression(openBracketToken)

internal class SymbolTypeExpression(
    val symbolToken: Token.Symbol,
) : TypeExpression(symbolToken)
