package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseParenthesisedExpression(
    parseUntilCondition: (Token) -> Boolean,
): ParenthesisedExpression {
    val openBracketToken = expectToken<Token.BracketOpen>("parenthesised expression")
    val expression = parseExpression(0f) { parseUntilCondition(it) || it is Token.BracketClose }
        ?: throw UnexpectedTokenException("expression", getToken(), "parenthesised expression")
    val closeBracketToken = expectToken<Token.BracketClose>("parenthesised expression")
    return ParenthesisedExpression(
        openBracketToken = openBracketToken,
        closeBracketToken = closeBracketToken,
        expression = expression,
    )
}
