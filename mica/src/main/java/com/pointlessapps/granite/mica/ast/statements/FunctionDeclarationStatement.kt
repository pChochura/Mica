package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that declares a function.
 *
 * Examples:
 *  ```
 *  add(a: int, b: real): int {
 *    otherMethod()
 *    return 1
 *  }
 *  ```
 *  ```
 *  method() {
 *    > "Hello"
 *  }
 *  ```
 *  ```
 *  method2() {
 *    return
 *    > "Statement that won't be reached"
 *  }
 *  ```
 *  ```
 *  methodWithDefaultParameters(a: int = 1, b: real = 2.0) {}
 *  ```
 *  ```
 *  // Only parameters at the end can have default values
 *  methodWithDefaultParameters(a: int, b: real = 2.0) {}
 *  ```
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
    val equalsToken: Token.Equals?,
    val defaultValueExpression: Expression?,
)
