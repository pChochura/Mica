package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that declares a function.
 * TODO
 *
 * If dealing with generic types, you can use the exclamation mark to mark the type as not
 * being used in the inferring process.
 *
 * In other cases the exclamation mark is optional but can mark the type to be matched exactly,
 * not by a subtype.
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
 *  // Valid calls
 *  methodWithDefaultParameters()
 *  methodWithDefaultParameters(5)
 *  methodWithDefaultParameters(5, 3.0)
 *  // Invalid calls
 *  methodWithDefaultParameters(5.0)
 *  ```
 *  ```
 *  // Only parameters at the end can have default values
 *  methodWithDefaultParameters(a: int, b: real = 2.0) {}
 *  // Valid calls
 *  methodWithDefaultParameters(5)
 *  methodWithDefaultParameters(5, 3.0)
 *  // Invalid calls
 *  methodWithDefaultParameters()
 *  ```
 *  ```
 *  // Only parameters at the end can be a vararg
 *  methodWithVarargParameters(a: int, ..b: [real]) {}
 *  // Valid calls
 *  methodWithVarargParameters(5)
 *  methodWithVarargParameters(5, 3.0)
 *  methodWithVarargParameters(5, 3.0, 4.0)
 *  // Invalid calls
 *  methodWithVarargParameters()
 *  methodWithVarargParameters(5, 5)
 *  ```
 *  ```
 *  // @[char] indicates a constraint for the type argument that can be passed to the function
 *  // Accessing the type argument in the function body is allowed via the `type` keyword.
 *  // If the `type` keyword is used as a type for a parameter, the type argument can be inferred.
 *  indexOf@[char](input: type, element: char): int {
 *    loop item, index in input {
 *      if (item == element) {
 *        return index
 *      }
 *    }
 *
 *    return -1
 *  }
 *  // Valid calls
 *  indexOf@string("Hello", 'a')
 *  indexOf@[char](['H', 'e', 'l', 'l', 'o'], 'o')
 *  indexOf@{char}({'H', 'e', 'l', 'l', 'o'}, 'o')
 *  indexOf("Inferred Hello", 'o')
 *  // Invalid calls
 *  // `int` is not a subtype of `char` and the argument doesn't match the type argument
 *  indexOf@[int](["H", "e", "l", "l", "o"], 'o')
 *  // `any` is not a subtype of `[char]`
 *  indexOf@any(["H", "e", "l", "l", "o"], 'o')
 *  ```
 *  ```
 *  // This method can only be called as a global function
 *  add!(a: int, b: int): int {
 *    return a + b
 *  }
 *
 *  add(34, 35) // correct
 *  34.add(35) // incorrect
 *  ```
 *  TODO
 */
internal data class FunctionDeclarationStatement(
    val nameToken: Token.Symbol,
    val openBracketToken: Token.BracketOpen,
    val closeBracketToken: Token.BracketClose,
    val openCurlyToken: Token.CurlyBracketOpen,
    val closeCurlyToken: Token.CurlyBracketClose,
    val exclamationMarkToken: Token.Operator?,
    val atToken: Token.At?,
    val typeParameterConstraint: TypeExpression?,
    val colonToken: Token.Colon?,
    val returnTypeExpression: TypeExpression?,
    val parameters: List<FunctionParameterDeclarationStatement>,
    val body: List<Statement>,
) : Statement(nameToken)

internal class FunctionParameterDeclarationStatement(
    val varargToken: Token.Operator?,
    val nameToken: Token.Symbol,
    val colonToken: Token.Colon,
    val typeExpression: TypeExpression,
    val exclamationMarkToken: Token.Operator?,
    val equalsToken: Token.Equals?,
    val defaultValueExpression: Expression?,
)
