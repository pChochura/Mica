package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expressions that have a left and right side and an operator in between.
 * Examples:
 *  - `1 + 2`
 *  - `a - b`
 *  - `true | false`
 *  - `a == b`
 *  - `a != 123`
 */
internal class BinaryExpression(
    val lhs: Expression,
    val operatorToken: Token.Operator,
    val rhs: Expression,
) : Expression(lhs.startingToken)
