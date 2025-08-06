package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression

/**
 * Statement that consists of a single function call expression.
 *
 * @see [FunctionCallExpression]
 */
internal class FunctionCallStatement(
    val functionCallExpression: FunctionCallExpression,
) : Statement(functionCallExpression.startingToken)
