package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

internal class AssignmentStatement(
    val lhsToken: Token.Symbol,
    val equalSignToken: Token.Equals,
    val rhs: Expression,
) : Statement(lhsToken)
