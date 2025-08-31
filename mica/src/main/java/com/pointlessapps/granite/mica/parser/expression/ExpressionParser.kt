package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseExpression(
    minBindingPower: Float = 0f,
    parseUntilCondition: (Token) -> Boolean,
): Expression? {
    var lhs = when (getToken()) {
        is Token.Symbol -> parseSymbolExpression(parseUntilCondition)
        is Token.NumberLiteral -> NumberLiteralExpression(expectToken("number literal"))
        is Token.BooleanLiteral -> BooleanLiteralExpression(expectToken("boolean literal"))
        is Token.CharLiteral -> CharLiteralExpression(expectToken("char literal"))
        is Token.StringLiteral -> StringLiteralExpression(expectToken("string literal"))
        is Token.Operator -> parseUnaryExpression(parseUntilCondition)
        is Token.BracketOpen -> parseParenthesisedExpression(parseUntilCondition)
        is Token.SquareBracketOpen -> parseArrayLiteralExpression(parseUntilCondition)
        is Token.CurlyBracketOpen -> parseSetLiteralExpression(parseUntilCondition)
        is Token.Increment, is Token.Decrement -> parsePrefixAssignmentExpression()
        else -> throw UnexpectedTokenException("expression", getToken(), "expression")
    }

    while (!parseUntilCondition(getToken())) {
        val currentToken = getToken()

        if (currentToken is Token.SquareBracketOpen) {
            val arrayIndex = parseArrayIndexExpression(lhs, minBindingPower, parseUntilCondition)
            if (arrayIndex == null) break

            lhs = arrayIndex
            continue
        }

        if (currentToken is Token.Dot) {
            val memberFunctionCall = parseMemberAccessExpression(
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
            throw UnexpectedTokenException(")", getToken(), "expression")
        }

        advance()

        val rhs = parseExpression(rbp, parseUntilCondition)
            ?: throw UnexpectedTokenException("expression", getToken(), "expression")

        lhs = BinaryExpression(lhs, currentToken, rhs)
    }

    return lhs
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
        else -> throw UnexpectedTokenException("binary operator", token, "expression")
    }

    is Token.BracketClose -> 0f to 0f
    else -> throw UnexpectedTokenException("binary operator or )", token, "expression")
}

internal fun getPostfixBindingPower(token: Token): Float = when (token) {
    is Token.SquareBracketOpen -> 17f
    is Token.Dot -> 18f
    else -> throw UnexpectedTokenException("[ or .", token, "expression")
}
