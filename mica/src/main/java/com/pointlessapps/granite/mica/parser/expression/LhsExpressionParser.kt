package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Helper.isFunctionCallStatementStarting
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseLhsExpression(
    parseUntilCondition: (Token) -> Boolean,
) = when (val token = getToken()) {
    is Token.Symbol -> parseSymbolExpression(parseUntilCondition, token)
    is Token.NumberLiteral -> NumberLiteralExpression(token).also { advance() }
    is Token.BooleanLiteral -> BooleanLiteralExpression(token).also { advance() }
    is Token.CharLiteral -> CharLiteralExpression(token).also { advance() }
    is Token.StringLiteral -> StringLiteralExpression(token).also { advance() }
    is Token.Operator -> parseUnaryExpression(token, parseUntilCondition)
    is Token.BracketOpen -> parseParenthesisedExpression(parseUntilCondition)
    is Token.SquareBracketOpen -> parseArrayLiteralExpression(parseUntilCondition)
    else -> null
}

private fun Parser.parseSymbolExpression(
    parseUntilCondition: (Token) -> Boolean,
    token: Token.Symbol,
): Expression = when {
    isFunctionCallStatementStarting() -> parseFunctionCallExpression(parseUntilCondition)
    else -> SymbolExpression(token).also { advance() }
}

private fun Parser.parseUnaryExpression(
    token: Token.Operator,
    parseUntilCondition: (Token) -> Boolean,
): UnaryExpression {
    advance()
    val rbp = getPrefixBindingPower(token)
    val expression = parseExpression(rbp, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken())

    return UnaryExpression(token, expression)
}

private fun Parser.parseParenthesisedExpression(parseUntilCondition: (Token) -> Boolean): ParenthesisedExpression {
    val openBracketToken = expectToken<Token.BracketOpen>()
    val expression = parseExpression(0f) { parseUntilCondition(it) || it is Token.BracketClose }
        ?: throw UnexpectedTokenException("expression", getToken())
    val closeBracketToken = expectToken<Token.BracketClose>()
    return ParenthesisedExpression(
        openBracketToken = openBracketToken,
        closeBracketToken = closeBracketToken,
        expression = expression,
    )
}

private fun getPrefixBindingPower(token: Token): Float = when (token) {
    is Token.Operator -> when (token.type) {
        Token.Operator.Type.Not -> 11f
        Token.Operator.Type.Add, Token.Operator.Type.Subtract -> 9.5f
        else -> throw UnexpectedTokenException("binary operator", token)
    }

    is Token.BracketOpen -> 0.5f
    else -> throw UnexpectedTokenException("prefix unary operator or (", token)
}
