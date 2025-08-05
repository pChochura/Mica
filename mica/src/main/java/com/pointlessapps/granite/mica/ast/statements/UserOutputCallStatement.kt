package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

internal class UserOutputCallStatement(
    val outputToken: Token.Operator,
    val contentExpression: Expression,
) : Statement(outputToken)
