package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

internal class VariableDeclarationStatement(
    val lhsToken: Token.Symbol,
    val colonToken: Token.Colon,
    val typeToken: Token.Symbol,
    val equalSignToken: Token.Equals,
    val rhs: Expression,
) : Statement()
