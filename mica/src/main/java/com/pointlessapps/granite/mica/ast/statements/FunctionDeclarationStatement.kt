package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

internal class FunctionDeclarationStatement(
    val nameToken: Token.Symbol,
    val openBracketToken: Token.BracketOpen,
    val closeBracketToken: Token.BracketClose,
    val openCurlyToken: Token.CurlyBracketOpen,
    val closeCurlyToken: Token.CurlyBracketClose,
    val colonToken: Token.Colon?,
    val returnTypeToken: Token.Symbol?,
    val parameters: List<FunctionParameterDeclarationStatement>,
    val body: List<Statement>,
) : Statement()
