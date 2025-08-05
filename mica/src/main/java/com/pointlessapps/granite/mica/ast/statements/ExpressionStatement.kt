package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression

internal class ExpressionStatement(
    val expression: Expression,
) : Statement(expression.startingToken)
