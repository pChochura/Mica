package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.isFunctionCallExpressionStarting
import com.pointlessapps.granite.mica.parser.statement.parseArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.parser.statement.parsePropertyAccessAccessorExpression

internal fun Parser.parseMemberAccessExpression(
    lhs: Expression,
    minBindingPower: Float,
    parseUntilCondition: (Token) -> Boolean,
): Expression? {
    val lbp = getPostfixBindingPower(getToken())
    if (lbp < minBindingPower) {
        return null
    }

    val accessorExpressions = mutableListOf<AccessorExpression>()

    fun getCurrentLhs() = if (accessorExpressions.isEmpty()) {
        lhs
    } else {
        MemberAccessExpression(
            symbolExpression = lhs,
            accessorExpressions = accessorExpressions,
        )
    }

    while (isToken<Token.SquareBracketOpen>() || isToken<Token.Dot>()) {
        val accessorExpression = if (isToken<Token.SquareBracketOpen>()) {
            parseArrayIndexAccessorExpression(parseUntilCondition)
        } else if (isFunctionCallExpressionStarting()) {
            expectToken<Token.Dot>("member function call expression")
            val functionCallExpression = parseFunctionCallExpression(parseUntilCondition)

            return FunctionCallExpression(
                nameToken = functionCallExpression.nameToken,
                openBracketToken = functionCallExpression.openBracketToken,
                closeBracketToken = functionCallExpression.closeBracketToken,
                arguments = listOf(getCurrentLhs()).plus(functionCallExpression.arguments),
                isMemberFunctionCall = true,
            )
        } else if (isToken<Token.Dot>()) {
            parsePropertyAccessAccessorExpression()
        } else {
            throw UnexpectedTokenException(
                expectedToken = "array index or property access",
                actualToken = getToken(),
                currentlyParsing = "member access expression",
            )
        }

        accessorExpressions.add(accessorExpression)
    }

    return getCurrentLhs()
}
