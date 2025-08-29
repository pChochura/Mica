package com.pointlessapps.granite.mica.ast

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

/**
 * Helper class that holds information about the index of an array-like variable.
 *
 * Example:
 *  - `[1]`
 *  - `[method() + 20]`
 */
internal class ArrayIndex(
    val openBracketToken: Token.SquareBracketOpen,
    val closeBracketToken: Token.SquareBracketClose,
    val expression: Expression,
)
