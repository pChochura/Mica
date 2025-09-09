package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseMemberAccessExpression(
    lhs: Expression,
    minBindingPower: Float,
    parseUntilCondition: (Token) -> Boolean,
): Expression? {
    val lbp = getPostfixBindingPower(getToken())
    if (lbp < minBindingPower) {
        return null
    }

    val accessorExpressions = parseAccessorExpressions(parseUntilCondition)
    val memberFunctionCallExpression = parseMemberFunctionCallExpression(
        lhs = lhs,
        accessorExpressions = accessorExpressions,
        parseUntilCondition = parseUntilCondition,
    )

    return memberFunctionCallExpression ?: MemberAccessExpression(
        symbolExpression = lhs,
        accessorExpressions = accessorExpressions,
    )
}
