package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.isFunctionCallStatementStarting
import com.pointlessapps.granite.mica.parser.isPostfixAssignmentExpressionStarting

internal fun Parser.parseSymbolExpression(
    parseUntilCondition: (Token) -> Boolean,
): Expression = when {
    getToken().let { it is Token.Keyword && it.value == Keyword.IF.value } ->
        parseIfConditionExpression(parseUntilCondition)

    isFunctionCallStatementStarting() -> parseFunctionCallExpression(parseUntilCondition)
    isPostfixAssignmentExpressionStarting() -> parsePostfixAssignmentExpression(parseUntilCondition)
    else -> SymbolExpression(expectToken<Token.Symbol>("symbol expression") { it !is Token.Keyword })
}
