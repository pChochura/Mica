package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression

/**
 * Statement that consists of a single expression.
 *
 * @see [Expression]
 */
internal class ExpressionStatement(
    val expression: Expression,
) : Statement(expression.startingToken)
