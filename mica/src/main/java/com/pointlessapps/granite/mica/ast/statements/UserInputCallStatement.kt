package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

internal class UserInputCallStatement(
    val startingToken: Token.Operator,
    val contentToken: Token.Symbol,
) : Statement()
