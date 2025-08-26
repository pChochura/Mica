package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that declares a custom type.
 *
 * Examples:
 *  ```
 *  type intPair {
 *    first: int
 *    second: int
 *
 *    dist() : int {
 *      return first + second
 *    }
 *  }
 *  ```
 *  ```
 *  type extendedBool {
 *    value: bool
 *  }
 *  ```
 */
internal class TypeDeclarationStatement(
    val typeToken: Token.Keyword,
    val nameToken: Token.Symbol,
    val openCurlyToken: Token.CurlyBracketOpen,
    val closeCurlyToken: Token.CurlyBracketClose,
    val properties: List<TypePropertyDeclarationStatement>,
    val functions: List<FunctionDeclarationStatement>,
) : Statement(typeToken)

internal class TypePropertyDeclarationStatement(
    val nameToken: Token.Symbol,
    val colonToken: Token.Colon,
    val typeExpression: TypeExpression,
)
