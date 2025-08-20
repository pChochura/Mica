package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.ArrayIndexExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseExpression(
    minBindingPower: Float = 0f,
    parseUntilCondition: (Token) -> Boolean,
): Expression? {
    var lhs = parseLhsExpression(parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken())

    while (!parseUntilCondition(getToken())) {
        val currentToken = getToken()

        if (currentToken is Token.SquareBracketOpen) {
            val arrayIndex = parseArrayIndexExpression(lhs, minBindingPower, parseUntilCondition)
            if (arrayIndex == null) break

            lhs = arrayIndex
            continue
        }

        if (currentToken is Token.Dot) {
            val memberFunctionCall = parseMemberFunctionCallExpression(
                lhs = lhs,
                minBindingPower = minBindingPower,
                parseUntilCondition = parseUntilCondition,
            )
            if (memberFunctionCall == null) break

            lhs = memberFunctionCall
            continue
        }

        if (
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
    }

    return lhs
}

private fun Parser.parseArrayIndexExpression(
    lhs: Expression,
    minBindingPower: Float = 0f,
    parseUntilCondition: (Token) -> Boolean,
): ArrayIndexExpression? {
    val lbp = getPostfixBindingPower(getToken())
    if (lbp < minBindingPower) {
        return null
    }

    val openBracketToken = expectToken<Token.SquareBracketOpen>()
    val indexExpression = parseExpression {
        parseUntilCondition(it) || it is Token.SquareBracketClose
    } ?: throw UnexpectedTokenException("expression", getToken())
    val closeBracketToken = expectToken<Token.SquareBracketClose>()

    return ArrayIndexExpression(
        arrayExpression = lhs,
        openBracketToken = openBracketToken,
        closeBracketToken = closeBracketToken,
        indexExpression = indexExpression,
    )
}

private fun Parser.parseMemberFunctionCallExpression(
    lhs: Expression,
    minBindingPower: Float = 0f,
    parseUntilCondition: (Token) -> Boolean,
): FunctionCallExpression? {
    val lbp = getPostfixBindingPower(getToken())
    if (lbp < minBindingPower) {
        return null
    }

    expectToken<Token.Dot>()
    val functionCallExpression = parseFunctionCallExpression(parseUntilCondition)

    return FunctionCallExpression(
        nameToken = functionCallExpression.nameToken,
        openBracketToken = functionCallExpression.openBracketToken,
        closeBracketToken = functionCallExpression.closeBracketToken,
        arguments = listOf(lhs).plus(functionCallExpression.arguments),
    )
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

private fun getPostfixBindingPower(token: Token): Float = when (token) {
    is Token.SquareBracketOpen -> 17f
    is Token.Dot -> 18f
    else -> throw UnexpectedTokenException("[ or .", token)
}
