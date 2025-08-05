package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token

internal fun Parser.parseExpression(
    minBindingPower: Float = 0f,
    parseUntilCondition: (Token) -> Boolean = { it is Token.EOL || it is Token.EOF },
): Expression? {
    var lhs = parseExpressionLhs() ?: throw UnexpectedTokenException("expression", getToken())

    if (parseUntilCondition(getToken())) {
        return lhs
    }

    do {
        val currentToken = getToken()

        if (
            parseUntilCondition(currentToken) ||
            currentToken is Token.BracketClose ||
            currentToken !is Token.Operator
        ) {
            break
        }

        val (lbp, rbp) = getInfixBindingPowers(currentToken)
        if (lbp <= minBindingPower) {
            break
        }

        if (isToken<Token.BracketClose>()) {
            throw UnexpectedTokenException(")", getToken())
        }

        advance()

        val rhs = parseExpression(rbp) ?: throw UnexpectedTokenException("expression", getToken())

        lhs = BinaryExpression(lhs, currentToken, rhs)
    } while (!parseUntilCondition(getToken()))

    return lhs
}

private fun Parser.parseExpressionLhs() = when (val token = getToken()) {
    is Token.Symbol -> parseInSequence(
        ::parseFunctionCallExpression,
        { SymbolExpression(token).also { advance() } },
    )

    is Token.NumberLiteral -> NumberLiteralExpression(token).also { advance() }
    is Token.BooleanLiteral -> BooleanLiteralExpression(token).also { advance() }
    is Token.StringLiteral -> StringLiteralExpression(token).also { advance() }
    is Token.Operator -> {
        advance()
        val rbp = getPrefixBindingPowers(token)
        val expression =
            parseExpression(rbp) ?: throw UnexpectedTokenException("expression", getToken())

        UnaryExpression(token, expression)
    }

    is Token.BracketOpen -> {
        val openBracketToken = expectToken<Token.BracketOpen>()
        val expression = parseExpression(0f) { it is Token.BracketClose }
            ?: throw UnexpectedTokenException("expression", getToken())
        val closeBracketToken = expectToken<Token.BracketClose>()
        ParenthesisedExpression(
            openBracketToken = openBracketToken,
            closeBracketToken = closeBracketToken,
            expression = expression,
        )
    }

    else -> null
}

internal fun Parser.parseFunctionCallExpression(): FunctionCallExpression {
    val nameToken = expectToken<Token.Symbol>()
    val openBracketToken = expectToken<Token.BracketOpen>()
    val arguments = mutableListOf<Expression>()
    while (!isToken<Token.BracketClose>()) {
        val argument = parseExpression(
            parseUntilCondition = { it is Token.Comma || it is Token.BracketClose },
        ) ?: throw UnexpectedTokenException("expression", getToken())

        arguments.add(argument)

        if (isToken<Token.Comma>()) {
            advance()

            assert(!isToken<Token.BracketClose>()) {
                throw UnexpectedTokenException("expression", getToken())
            }
        }
    }
    val closeBracketToken = expectToken<Token.BracketClose>()

    return FunctionCallExpression(nameToken, openBracketToken, closeBracketToken, arguments)
}

private fun getInfixBindingPowers(token: Token): Pair<Float, Float> = when (token) {
    is Token.Operator -> when (token.type) {
        Token.Operator.Type.Or -> 1f to 2f
        Token.Operator.Type.And -> 3f to 4f
        Token.Operator.Type.Equals, Token.Operator.Type.NotEquals -> 5f to 6f
        Token.Operator.Type.GraterThan, Token.Operator.Type.LessThan,
        Token.Operator.Type.GraterThanOrEquals, Token.Operator.Type.LessThanOrEquals,
            -> 7f to 8f

        Token.Operator.Type.Add, Token.Operator.Type.Subtract -> 9f to 10f
        Token.Operator.Type.Multiply, Token.Operator.Type.Divide -> 12f to 11f
        Token.Operator.Type.Exponent -> 13f to 14f
        Token.Operator.Type.Range -> 15f to 16f
        else -> throw UnexpectedTokenException("binary operator", token)
    }

    is Token.BracketClose -> 0f to 0f
    else -> throw UnexpectedTokenException("binary operator or )", token)
}

private fun getPrefixBindingPowers(token: Token): Float = when (token) {
    is Token.Operator -> when (token.type) {
        Token.Operator.Type.Not -> 11f
        Token.Operator.Type.Add, Token.Operator.Type.Subtract -> 9.5f
        else -> throw UnexpectedTokenException("binary operator", token)
    }

    is Token.BracketOpen -> 0.5f
    else -> throw UnexpectedTokenException("binary operator or (", token)
}
