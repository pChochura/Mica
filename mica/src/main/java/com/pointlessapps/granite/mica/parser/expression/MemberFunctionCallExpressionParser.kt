package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseMemberFunctionCallExpression(
    lhs: Expression,
    minBindingPower: Float,
    parseUntilCondition: (Token) -> Boolean,
): FunctionCallExpression? {
    val lbp = getPostfixBindingPower(getToken())
    if (lbp < minBindingPower) {
        return null
    }

    expectToken<Token.Dot>("member function call expression")
    val functionCallExpression = parseFunctionCallExpression(parseUntilCondition)

    return FunctionCallExpression(
        nameToken = functionCallExpression.nameToken,
        openBracketToken = functionCallExpression.openBracketToken,
        closeBracketToken = functionCallExpression.closeBracketToken,
        arguments = listOf(lhs).plus(functionCallExpression.arguments),
        isMemberFunctionCall = true,
    )
}
