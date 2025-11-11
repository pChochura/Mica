package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that represents an if condition. It can be used as a standalone statement.
 * The curly brackets are optional if there is only a one statement.
 * Whenever it is used as an expression, the last [ExpressionStatement] in the body is used as
 * the result of the expression. In those cases the if expression must be exhaustive (meaning that
 * the else declaration is mandatory).
 *
 * Examples:
 *  ```
 *  if a == 1 {
 *    // Body of the if statement
 *  }
 *  ```
 *  ```
 *  result = if a == 1 {
 *    // Body of the if statement
 *    "value when the a == 1"
 *  } else {
 *    // Body of the else statement
 *    "value in the other cases"
 *  }
 *  ```
 *  ```
 *  result = if a == 1 {
 *    // Body of the if statement
 *    "first result"
 *  } else if b == 2 {
 *    // Body of the else if statement
 *    "second result"
 *  } else {
 *    // Body of the else statement
 *    "third result"
 *  }
 *  ```
 *  ```
 *  if a == 1
 *    // Body of the if statement
 *  else // Body of the else statement
 *  ```
 *  ```
 *  result = if a < 10 fun() else otherFun()
 *  ```
 *
 *  The control flow altering statements can be used inside of the expression:
 *  ```
 *  loop {
 *    // "break" will break out of the loop, so the value "" won't be used
 *    // But it has to be provided so that the result of the expression is defined
 *    result: string = if a == 1 { break "" } else "value"
 *  }
 *  ```
 */
internal class IfConditionExpression(
    val ifConditionDeclaration: IfConditionDeclaration,
    val elseIfConditionDeclarations: List<ElseIfConditionDeclaration>?,
    val elseDeclaration: ElseDeclaration?,
) : Expression(ifConditionDeclaration.ifToken)

internal class IfConditionDeclaration(
    val ifToken: Token.Keyword,
    val ifConditionExpression: Expression,
    val ifBody: BlockBody,
)

internal class ElseIfConditionDeclaration(
    val elseIfToken: Pair<Token.Keyword, Token.Keyword>,
    val elseIfConditionExpression: Expression,
    val elseIfBody: BlockBody,
)

internal class ElseDeclaration(
    val elseToken: Token.Keyword,
    val elseBody: BlockBody,
)

internal class BlockBody(
    val openCurlyToken: Token.CurlyBracketOpen?,
    val closeCurlyToken: Token.CurlyBracketClose?,
    val statements: List<Statement>,
)
