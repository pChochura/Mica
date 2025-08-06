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
    val ifToken: Token.Keyword,
    val conditionExpression: Expression,
    val openCurlyToken: Token.CurlyBracketOpen?,
    val closeCurlyToken: Token.CurlyBracketClose?,
    val body: List<Statement>,
    val elseIfConditionStatements: List<ElseIfConditionStatement>?,
    val elseStatement: ElseStatement?,
) : Statement(ifToken)

internal class ElseIfConditionStatement(
    val elseIfToken: Pair<Token.Keyword, Token.Keyword>,
    val elseIfConditionExpression: Expression,
    val elseIfOpenCurlyToken: Token.CurlyBracketOpen?,
    val elseIfCloseCurlyToken: Token.CurlyBracketClose?,
    val elseIfBody: List<Statement>,
)

internal class ElseStatement(
    val elseToken: Token.Keyword,
    val elseOpenCurlyToken: Token.CurlyBracketOpen?,
    val elseCloseCurlyToken: Token.CurlyBracketClose?,
    val elseBody: List<Statement>,
)
