package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

internal class UserOutputCallStatement(
    val startingToken: Token.GraterThan,
    val contentToken: Token.StringLiteral,
) : Statement()
