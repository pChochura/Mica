package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.TypeCoercionExpression
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseTypeCoercionExpression(
    lhs: Expression,
    minBindingPower: Float,
    parseUntilCondition: (Token) -> Boolean,
): TypeCoercionExpression? {
    val lbp = getPostfixBindingPower(getToken())
    if (lbp < minBindingPower) {
        return null
    }

    val asToken = expectToken<Token.Keyword>("type coercion expression") {
        it.value == Keyword.AS.value
    }
    val typeExpression = parseTypeExpression(parseUntilCondition)

    return TypeCoercionExpression(
        lhs = lhs,
        asToken = asToken,
        typeExpression = typeExpression,
    )
}
