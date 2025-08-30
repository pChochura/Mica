package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.SetLiteralExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseSetLiteralExpression(
    parseUntilCondition: (Token) -> Boolean,
): SetLiteralExpression {
    val openBracketToken = expectToken<Token.CurlyBracketOpen>("set literal expression")
    val elements = mutableListOf<Expression>()
    while (!isToken<Token.CurlyBracketClose>()) {
        val element = parseExpression {
            parseUntilCondition(it) || it is Token.Comma || it is Token.CurlyBracketClose
        } ?: throw UnexpectedTokenException("expression", getToken(), "set literal expression")

        elements.add(element)

        if (isToken<Token.Comma>()) {
            advance()

            assert(!isToken<Token.CurlyBracketClose>()) {
                throw UnexpectedTokenException("expression", getToken(), "set literal expression")
            }
        }
    }
    val closeBracketToken = expectToken<Token.CurlyBracketClose>("set literal expression")

    return SetLiteralExpression(openBracketToken, closeBracketToken, elements)
}
