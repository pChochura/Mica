package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that declares a function.
 *
 * Examples:
 *  - `add(a: int, b: real): int { return 1 }`
 *  - `method() {}`
 *  - `method2() { return }`
 */
internal data class FunctionDeclarationStatement(
    val nameToken: Token.Symbol,
    val openBracketToken: Token.BracketOpen,
    val closeBracketToken: Token.BracketClose,
    val openCurlyToken: Token.CurlyBracketOpen,
    val closeCurlyToken: Token.CurlyBracketClose,
    val colonToken: Token.Colon?,
    val returnTypeExpression: TypeExpression?,
    val parameters: List<FunctionParameterDeclarationStatement>,
    val body: List<Statement>,
) : Statement(nameToken)

internal class FunctionParameterDeclarationStatement(
    val nameToken: Token.Symbol,
    val colonToken: Token.Colon,
    val typeExpression: TypeExpression,
)
