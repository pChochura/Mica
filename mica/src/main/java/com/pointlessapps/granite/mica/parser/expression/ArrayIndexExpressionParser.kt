package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.ArrayIndexExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseArrayIndexExpression(
    lhs: Expression,
    minBindingPower: Float,
    parseUntilCondition: (Token) -> Boolean,
): ArrayIndexExpression? {
    val lbp = getPostfixBindingPower(getToken())
    if (lbp < minBindingPower) {
        return null
    }

    val openBracketToken = expectToken<Token.SquareBracketOpen>("array index expression")
    val indexExpression = parseExpression {
        parseUntilCondition(it) || it is Token.SquareBracketClose
    } ?: throw UnexpectedTokenException("expression", getToken(), "array index expression")
    val closeBracketToken = expectToken<Token.SquareBracketClose>("array index expression")

    return ArrayIndexExpression(
        arrayExpression = lhs,
        openBracketToken = openBracketToken,
        closeBracketToken = closeBracketToken,
        indexExpression = indexExpression,
    )
}
