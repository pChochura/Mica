package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that encapsulates a body that will be executed while the condition is truthy.
 * If the condition is false, the else declaration body will be executed if provided.
 *
 * If the if condition isn't provided, the body will be executed forever.
 *
 * The curly brackets are optional if there is only a one statement.
 *
 * Examples:
 *  ```
 *  loop if a == 1 {
 *    // A statement that will be called as long as a == 1
 *  }
 *  ```
 *  ```
 *  loop if a == 1
 *    // A single line statement
 *  ```
 *  ```
 *  loop if false {
 *    // This won't be executed
 *  } else {
 *    // This will be executed
 *  }
 *  ```
 *  ```
 *  loop if true {
 *    if a == 1
 *      break
 *  }
 *  ```
 *  ```
 *  loop {
 *    // This will be executed forever
 *  }
 *  ```
 *  ```
 *  loop a in array {
 *    // A statement that will be called for each item in the array
 *  }
 *  ```
 *  ```
 *  loop a, index in array {
 *    // A statement that will be called for each item in the array
 *    // Additionally the index of the item can be accessed
 *  }
 *  ```
 */
internal sealed class LoopStatement(
    val loopToken: Token.Keyword,
    val loopBody: BlockBody,
) : Statement(loopToken)

internal class LoopIfStatement(
    loopToken: Token.Keyword,
    val ifToken: Token.Keyword?,
    val ifConditionExpression: Expression?,
    loopBody: BlockBody,
    val elseDeclaration: ElseDeclaration?,
) : LoopStatement(loopToken, loopBody)

internal class LoopInStatement(
    loopToken: Token.Keyword,
    val symbolToken: Token.Symbol,
    val commaToken: Token.Comma?,
    val indexToken: Token.Symbol?,
    val inToken: Token.Keyword?,
    val arrayExpression: Expression,
    loopBody: BlockBody,
) : LoopStatement(loopToken, loopBody)
