package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

internal class ReturnStatement(
    val returnToken: Token.Keyword,
    val returnExpression: Expression?,
) : Statement()
