package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.isFunctionCallStatementStarting

internal fun Parser.parseMemberAccessExpression(
    lhs: Expression,
    minBindingPower: Float,
    parseUntilCondition: (Token) -> Boolean,
): Expression? {
    val lbp = getPostfixBindingPower(getToken())
    if (lbp < minBindingPower) {
        return null
    }

    val dotToken = expectToken<Token.Dot>("member access expression")

    if (isFunctionCallStatementStarting()) {
        val functionCallExpression = parseFunctionCallExpression(parseUntilCondition)

        return FunctionCallExpression(
            nameToken = functionCallExpression.nameToken,
            openBracketToken = functionCallExpression.openBracketToken,
            closeBracketToken = functionCallExpression.closeBracketToken,
            arguments = listOf(lhs).plus(functionCallExpression.arguments),
            isMemberFunctionCall = true,
        )
    }

    return MemberAccessExpression(
        lhs = lhs,
        dotToken = dotToken,
        propertySymbolToken = expectToken<Token.Symbol>("member access expression") {
            it !is Token.Keyword
        },
    )
}
