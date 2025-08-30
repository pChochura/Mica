package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that represents an if condition.
 * The curly brackets are optional if there is only a one statement.
 *
 * Examples:
 *  ```
 *  if a == 1 {
 *    // Body of the if statement
 *  }
 *  ```
 *  ```
 *  if a == 1 {
 *    // Body of the if statement
 *  } else {
 *    // Body of the else statement
 *  }
 *  ```
 *  ```
 *  if a == 1 {
 *    // Body of the if statement
 *  } else if b == 2 {
 *    // Body of the else if statement
 *  } else {
 *    // Body of the else statement
 *  }
 *  ```
 *  ```
 *  if a == 1
 *    // Body of the if statement
 *  else // Body of the else statement
 *  ```
 */
internal class IfConditionStatement(
    val ifConditionDeclaration: IfConditionDeclaration,
    val elseIfConditionDeclarations: List<ElseIfConditionDeclaration>?,
    val elseDeclaration: ElseDeclaration?,
) : Statement(ifConditionDeclaration.ifToken)

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
