package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression

internal class FunctionCallStatement(
    val functionCallExpression: FunctionCallExpression,
) : Statement()
