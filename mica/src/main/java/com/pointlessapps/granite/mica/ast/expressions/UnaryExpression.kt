package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that has a single operator and a right side.
 *
 * Examples:
 *  - `-1`
 *  - `!true`
 *  - `+a`
 */
internal class UnaryExpression(
    val operatorToken: Token.Operator,
    val rhs: Expression,
) : Expression(operatorToken)
