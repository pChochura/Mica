package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that declares a custom type.
 * If the type extends a different type, it has to override all of the properties of the parent type.
 *
 * Examples:
 *  ```
 *  type intPair {
 *    first: int = 12
 *    second: int
 *
 *    dist() : int {
 *      return this.first + this.second
 *    }
 *  }
 *  pair = intPair { second = 2 }
 *  > pair.dist()
 *  ```
 *  ```
 *  type extendedBool {
 *    value: bool = true
 *
 *    toString(): string {
 *      if value return "extendedTrue"
 *      return "extendedFalse"
 *    }
 *  }
 *  value = extendedBool{}
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
    val equalsToken: Token.Equals?,
    val defaultValueExpression: Expression?,
)
