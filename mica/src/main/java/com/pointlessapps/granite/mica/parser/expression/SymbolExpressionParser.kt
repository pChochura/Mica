package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.isFunctionCallStatementStarting
import com.pointlessapps.granite.mica.parser.isPostfixUnaryExpressionStarting

internal fun Parser.parseSymbolExpression(
    parseUntilCondition: (Token) -> Boolean,
): Expression = when {
    isFunctionCallStatementStarting() -> parseFunctionCallExpression(parseUntilCondition)
    isPostfixUnaryExpressionStarting() -> parsePostfixUnaryExpression()
    else -> SymbolExpression(expectToken<Token.Symbol>("symbol expression") { it !is Token.Keyword })
}
