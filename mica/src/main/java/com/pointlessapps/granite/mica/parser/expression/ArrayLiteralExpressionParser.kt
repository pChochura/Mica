package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

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
