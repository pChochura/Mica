package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.ast.PropertyAccessAccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseFunctionCallExpression(
    symbolToken: Token.Symbol,
    parseUntilCondition: (Token) -> Boolean,
): FunctionCallExpression {
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
        nameToken = symbolToken,
        openBracketToken = openBracketToken,
        closeBracketToken = closeBracketToken,
        arguments = arguments,
        isMemberFunctionCall = false,
    )
}

internal fun Parser.parseMemberFunctionCallExpression(
    lhs: Expression,
    accessorExpressions: List<AccessorExpression>,
    parseUntilCondition: (Token) -> Boolean,
): FunctionCallExpression? {
    val lastAccessor = accessorExpressions.lastOrNull()
    if (isToken<Token.BracketOpen>() && lastAccessor is PropertyAccessAccessorExpression) {
        val functionCallExpression = parseFunctionCallExpression(
            symbolToken = lastAccessor.propertySymbolToken,
            parseUntilCondition = parseUntilCondition,
        )

        val receiverExpressionArgument = if (accessorExpressions.size > 1) {
            MemberAccessExpression(
                symbolExpression = lhs,
                // Remove the last one as it was wrongly parsed as a property
                accessorExpressions = accessorExpressions.dropLast(1),
            )
        } else {
            lhs
        }

        return FunctionCallExpression(
            nameToken = functionCallExpression.nameToken,
            openBracketToken = functionCallExpression.openBracketToken,
            closeBracketToken = functionCallExpression.closeBracketToken,
            arguments = listOf(receiverExpressionArgument).plus(functionCallExpression.arguments),
            isMemberFunctionCall = true,
        )
    }

    return null
}
