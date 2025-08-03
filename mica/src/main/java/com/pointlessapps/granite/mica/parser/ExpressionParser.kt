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
    parseUntilCondition: (Token) -> Boolean = { it == Token.EOL || it == Token.EOF },
): Expression? {
    var lhs = parseExpressionLhs()
        ?: throw UnexpectedTokenException("Expected expression, but got ${getToken()}")

    if (parseUntilCondition(getToken())) {
        return lhs
    }

    do {
        val currentToken = getToken()

        if (
            parseUntilCondition(currentToken) ||
            currentToken == Token.BracketClose ||
            currentToken !is Token.Operator
        ) {
            break
        }

        val (lbp, rbp) = getInfixBindingPowers(currentToken)
        if (lbp <= minBindingPower) {
            break
        }

        if (currentToken == Token.BracketClose) {
            throw UnexpectedTokenException("Unmatched closing parenthesis")
        }

        advance()

        val rhs = parseExpression(rbp)
        if (rhs == null) {
            throw UnexpectedTokenException(
                "Expected expression after operator $currentToken, but got ${getToken()}",
            )
        }

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
        val expression = parseExpression(rbp)
        if (expression == null) {
            throw UnexpectedTokenException("Expected expression, but got $token")
        }

        UnaryExpression(token, expression)
    }

    Token.BracketOpen -> {
        advance()
        val expression = parseExpression(0f) { it == Token.BracketClose }
        if (expression == null) {
            throw UnexpectedTokenException("Expected expression inside parentheses, but got ${getToken()}")
        }
        expectToken<Token.BracketClose>()
        ParenthesisedExpression(expression)
    }

    else -> null
}

internal fun Parser.parseFunctionCallExpression(): FunctionCallExpression {
    val nameToken = expectToken<Token.Symbol>()
    val openBracketToken = expectToken<Token.BracketOpen>()
    val arguments = mutableListOf<Expression>()
    while (getToken() != Token.BracketClose) {
        val argument = parseExpression(
            parseUntilCondition = {
                it == Token.Comma || it == Token.BracketClose
            },
        )
        if (argument == null) {
            throw UnexpectedTokenException("Expected expression, but got ${getToken()}")
        }

        arguments.add(argument)

        if (getToken() == Token.Comma) {
            advance()

            assert(getToken() != Token.BracketClose) {
                throw UnexpectedTokenException(
                    "Expected an expression, but got ${getToken()}",
                )
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
        else -> throw UnexpectedTokenException("Unexpected operator $token")
    }

    Token.BracketClose -> 0f to 0f

    else -> throw UnexpectedTokenException("Unexpected token $token")
}

private fun getPrefixBindingPowers(token: Token): Float = when (token) {
    is Token.Operator -> when (token.type) {
        Token.Operator.Type.Not -> 11f
        Token.Operator.Type.Add, Token.Operator.Type.Subtract -> 9.5f
        else -> throw UnexpectedTokenException("Unexpected operator $token")
    }

    Token.BracketOpen -> 0.5f
    else -> throw UnexpectedTokenException("Unexpected token $token")
}
