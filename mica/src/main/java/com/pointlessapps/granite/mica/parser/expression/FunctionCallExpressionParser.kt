package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseFunctionCallExpression(
    parseUntilCondition: (Token) -> Boolean,
): FunctionCallExpression {
    val nameToken = expectToken<Token.Symbol>()
    val openBracketToken = expectToken<Token.BracketOpen>()
    val arguments = mutableListOf<Expression>()
    while (!isToken<Token.BracketClose>()) {
        val argument = parseExpression {
            parseUntilCondition(it) || it is Token.Comma || it is Token.BracketClose
        } ?: throw UnexpectedTokenException("expression", getToken())

        arguments.add(argument)

        if (isToken<Token.Comma>()) {
            advance()

            assert(!isToken<Token.BracketClose>()) {
                throw UnexpectedTokenException("expression", getToken())
            }
        }
    }
    val closeBracketToken = expectToken<Token.BracketClose>()

    return FunctionCallExpression(nameToken, openBracketToken, closeBracketToken, arguments)
}
