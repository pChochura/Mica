package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that encapsulates a body that will be executed while the condition is truthy.
 * If the condition is false, the else declaration body will be executed if provided.
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
 *  // TODO add break statement to the examples
 */
internal class LoopIfStatement(
    val loopToken: Token.Keyword,
    val ifConditionDeclaration: IfConditionDeclaration,
    val elseDeclaration: ElseDeclaration?,
) : Statement(loopToken)
