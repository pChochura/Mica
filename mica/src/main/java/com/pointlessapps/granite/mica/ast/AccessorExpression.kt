package com.pointlessapps.granite.mica.ast

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

internal sealed interface AccessorExpression

/**
 * Helper class that holds information about the index of an array-like variable.
 *
 * Example:
 *  - `[1]`
 *  - `[method() + 20]`
 */
internal class ArrayIndexAccessorExpression(
    val openBracketToken: Token.SquareBracketOpen,
    val closeBracketToken: Token.SquareBracketClose,
    val indexExpression: Expression,
) : AccessorExpression

/**
 * Helper class that holds information about the property of a variable.
 *
 * Example:
 *  - `.first`
 *  - `.propertyA`
 */
internal class PropertyAccessAccessorExpression(
    val dotToken: Token.Dot,
    val propertySymbolToken: Token.Symbol,
) : AccessorExpression
