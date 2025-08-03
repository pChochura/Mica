package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

internal class IfConditionStatement(
    val ifToken: Token.Keyword,
    val conditionExpression: Expression,
    val openCurlyToken: Token.CurlyBracketOpen?,
    val closeCurlyToken: Token.CurlyBracketClose?,
    val body: List<Statement>,
    val elseIfConditionStatements: List<ElseIfConditionStatement>?,
    val elseStatement: ElseStatement?,
) : Statement()

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
