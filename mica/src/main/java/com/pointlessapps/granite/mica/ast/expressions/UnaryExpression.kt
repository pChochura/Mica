package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expressions that have a single operator and a right side.
 * Examples:
 *  - `-1`
 *  - `!true`
 *  - `+a`
 */
internal class UnaryExpression(
    val operator: Token.Operator,
    val rhs: Expression,
) : Expression()
