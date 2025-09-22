package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseUnaryExpression(
    parseUntilCondition: (Token) -> Boolean,
): UnaryExpression {
    val operatorToken = expectToken<Token.Operator>("unary expression")
    val rbp = getPrefixBindingPower(operatorToken)
    val expression = parseExpression(rbp, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken(), "unary expression")

    return UnaryExpression(operatorToken, expression)
}

private fun getPrefixBindingPower(token: Token): Float = when (token) {
    is Token.Operator -> when (token.type) {
        Token.Operator.Type.Not -> 11f
        Token.Operator.Type.Add, Token.Operator.Type.Subtract -> 13f
        else -> throw UnexpectedTokenException("prefix unary operator or (", token, "unary expression")
    }

    is Token.BracketOpen -> 0.5f
    else -> throw UnexpectedTokenException("prefix unary operator or (", token, "unary expression")
}
