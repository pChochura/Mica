package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that declares a custom type.
 *
 * Examples:
 *  ```
 *  type intPair : any {
 *    first: int
 *    second: int
 *
 *    dist() : int {
 *      return first + second
 *    }
 *  }
 *  ```
 *  ```
 *  type extendedBool : bool {
 *    value: bool
 *
 *    // Has to implement the function that converts the value to a value with a base type.
 *    asBool() : bool {
 *      return value
 *    }
 *  }
 *  ```
 */
internal class TypeDeclarationStatement(
    val typeToken: Token.Keyword,
    val nameToken: Token.Symbol,
    val openCurlyToken: Token.CurlyBracketOpen,
    val closeCurlyToken: Token.CurlyBracketClose,
    val colonToken: Token.Colon,
    val baseTypeExpression: TypeExpression,
    val properties: List<TypePropertyDeclarationStatement>,
    val functions: List<FunctionDeclarationStatement>,
) : Statement(typeToken)

internal class TypePropertyDeclarationStatement(
    val nameToken: Token.Symbol,
    val colonToken: Token.Colon,
    val typeExpression: TypeExpression,
)
