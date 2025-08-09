package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Helper.isFunctionCallStatementStarting

internal fun Parser.parseExpression(
    minBindingPower: Float = 0f,
    parseUntilCondition: (Token) -> Boolean,
): Expression? {
    var lhs = parseExpressionLhs(parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken())

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

        val rhs = parseExpression(rbp, parseUntilCondition)
            ?: throw UnexpectedTokenException("expression", getToken())

        lhs = BinaryExpression(lhs, currentToken, rhs)
    } while (!parseUntilCondition(getToken()))

    return lhs
}

private fun Parser.parseExpressionLhs(
    parseUntilCondition: (Token) -> Boolean,
) = when (val token = getToken()) {
    is Token.Symbol -> when {
        isFunctionCallStatementStarting() -> parseFunctionCallExpression(parseUntilCondition)
        else -> SymbolExpression(token).also { advance() }
    }

    is Token.NumberLiteral -> NumberLiteralExpression(token).also { advance() }
    is Token.BooleanLiteral -> BooleanLiteralExpression(token).also { advance() }
    is Token.CharLiteral -> CharLiteralExpression(token).also { advance() }
    is Token.StringLiteral -> StringLiteralExpression(token).also { advance() }
    is Token.Operator -> {
        advance()
        val rbp = getPrefixBindingPowers(token)
        val expression = parseExpression(rbp, parseUntilCondition)
            ?: throw UnexpectedTokenException("expression", getToken())

        UnaryExpression(token, expression)
    }

    is Token.BracketOpen -> {
        val openBracketToken = expectToken<Token.BracketOpen>()
        val expression = parseExpression(0f) { parseUntilCondition(it) || it is Token.BracketClose }
            ?: throw UnexpectedTokenException("expression", getToken())
        val closeBracketToken = expectToken<Token.BracketClose>()
        ParenthesisedExpression(
            openBracketToken = openBracketToken,
            closeBracketToken = closeBracketToken,
            expression = expression,
        )
    }

    is Token.SquareBracketOpen -> parseArrayLiteralExpression(parseUntilCondition)

    else -> null
}

internal fun Parser.parseFunctionCallExpression(
    parseUntilCondition: (Token) -> Boolean,
): FunctionCallExpression {
    val nameToken = expectToken<Token.Symbol>()
    val openBracketToken = expectToken<Token.BracketOpen>()
    val arguments = mutableListOf<Expression>()
    while (!isToken<Token.BracketClose>()) {
        val argument = parseExpression {
            parseUntilCondition(it) || it is Token.Comma || it is Token.BracketClose
        } ?: throw UnexpectedTokenException("expression", getToken())

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

internal fun Parser.parseArrayLiteralExpression(
    parseUntilCondition: (Token) -> Boolean,
): ArrayLiteralExpression {
    val openBracketToken = expectToken<Token.SquareBracketOpen>()
    val elements = mutableListOf<Expression>()
    while (!isToken<Token.SquareBracketClose>()) {
        val element = parseExpression {
            parseUntilCondition(it) || it is Token.Comma || it is Token.SquareBracketClose
        } ?: throw UnexpectedTokenException("expression", getToken())

        elements.add(element)

        if (isToken<Token.Comma>()) {
            advance()

            assert(!isToken<Token.SquareBracketClose>()) {
                throw UnexpectedTokenException("expression", getToken())
            }
        }
    }
    val closeBracketToken = expectToken<Token.SquareBracketClose>()

    return ArrayLiteralExpression(openBracketToken, closeBracketToken, elements)
}

internal fun Parser.parseType(
    parseUntilCondition: (Token) -> Boolean,
): TypeExpression {
    if (isToken<Token.SquareBracketOpen>()) {
        // Parse as an array
        val openBracketToken = expectToken<Token.SquareBracketOpen>()
        val typeExpression = parseType { parseUntilCondition(it) || it is Token.SquareBracketClose }
        val closeBracketToken = expectToken<Token.SquareBracketClose>()

        return ArrayTypeExpression(
            openBracketToken = openBracketToken,
            closeBracketToken = closeBracketToken,
            typeExpression = typeExpression,
        )
    }

    val symbolToken = expectToken<Token.Symbol>()
    return SymbolTypeExpression(symbolToken)
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
