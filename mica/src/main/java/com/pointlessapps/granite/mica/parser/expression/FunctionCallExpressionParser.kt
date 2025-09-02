package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseFunctionCallExpression(
    parseUntilCondition: (Token) -> Boolean,
): FunctionCallExpression {
    val nameToken = expectToken<Token.Symbol>("function call expression") { it !is Token.Keyword }
    val openBracketToken = expectToken<Token.BracketOpen>("function call expression")
    skipTokens<Token.EOL>()
    val arguments = mutableListOf<Expression>()
    while (!isToken<Token.BracketClose>()) {
        val argument = parseExpression {
            parseUntilCondition(it) || it is Token.Comma || it is Token.BracketClose
        } ?: throw UnexpectedTokenException("expression", getToken(), "function call expression")

        arguments.add(argument)

        skipTokens<Token.EOL>()
        if (isToken<Token.Comma>()) advance()
        skipTokens<Token.EOL>()
    }
    val closeBracketToken = expectToken<Token.BracketClose>("function call expression")

    return FunctionCallExpression(
        nameToken = nameToken,
        openBracketToken = openBracketToken,
        closeBracketToken = closeBracketToken,
        arguments = arguments,
        isMemberFunctionCall = false,
    )
}
