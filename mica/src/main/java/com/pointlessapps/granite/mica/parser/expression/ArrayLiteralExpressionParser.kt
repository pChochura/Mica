package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseArrayLiteralExpression(
    parseUntilCondition: (Token) -> Boolean,
): ArrayLiteralExpression {
    val openBracketToken = expectToken<Token.SquareBracketOpen>("array literal expression")
    skipTokens<Token.EOL>()
    val elements = mutableListOf<Expression>()
    while (!isToken<Token.SquareBracketClose>()) {
        val element = parseExpression {
            parseUntilCondition(it) || it is Token.Comma || it is Token.SquareBracketClose
        } ?: throw UnexpectedTokenException("expression", getToken(), "array literal expression")

        elements.add(element)

        skipTokens<Token.EOL>()
        if (isToken<Token.Comma>()) {
            advance()
            skipTokens<Token.EOL>()

            assert(!isToken<Token.SquareBracketClose>()) {
                throw UnexpectedTokenException("expression", getToken(), "array literal expression")
            }
        }
    }
    val closeBracketToken = expectToken<Token.SquareBracketClose>("array literal expression")

    return ArrayLiteralExpression(openBracketToken, closeBracketToken, elements)
}
