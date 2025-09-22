package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that declares a custom type.
 * It also creates a "constructor" function with its name and all of the properties as parameters.
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
 *  pair = intPair(0, 2)
 *  > pair.dist()
 *  ```
 *  ```
 *  type extendedBool {
 *    value: bool
 *
 *    toString(): string {
 *      if value return "extendedTrue"
 *      return "extendedFalse"
 *    }
 *  }
 *  value = extendedBool(false)
 *  ```
 *  TODO
 */
internal class TypeDeclarationStatement(
    val typeToken: Token.Keyword,
    val nameToken: Token.Symbol,
    val atToken: Token.At?,
    val typeParameterConstraint: TypeExpression?,
    val colonToken: Token.Colon?,
    val parentTypeExpression: TypeExpression?,
    val openCurlyToken: Token.CurlyBracketOpen,
    val closeCurlyToken: Token.CurlyBracketClose,
    val properties: List<TypePropertyDeclaration>,
    val functions: List<FunctionDeclarationStatement>,
) : Statement(typeToken)

internal class TypePropertyDeclaration(
    val nameToken: Token.Symbol,
    val colonToken: Token.Colon,
    val typeExpression: TypeExpression,
)
