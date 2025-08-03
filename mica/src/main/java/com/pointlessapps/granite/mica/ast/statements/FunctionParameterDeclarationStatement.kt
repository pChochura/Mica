package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

internal class FunctionParameterDeclarationStatement(
    val nameToken: Token.Symbol,
    val colonToken: Token.Colon,
    val typeToken: Token.Symbol,
) : Statement()
