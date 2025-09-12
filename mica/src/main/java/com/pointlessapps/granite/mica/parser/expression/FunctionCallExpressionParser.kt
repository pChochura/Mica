package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.ast.PropertyAccessAccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseFunctionCallExpression(
    symbolToken: Token.Symbol,
    parseUntilCondition: (Token) -> Boolean,
): FunctionCallExpression {
    var atToken: Token.At? = null
    var typeArgument: TypeExpression? = null
    if (isToken<Token.At>()) {
        atToken = expectToken<Token.At>("function call type argument expression")
        typeArgument = parseTypeExpression {
            parseUntilCondition(it) || it is Token.Operator && it.type == Token.Operator.Type.GraterThan
        }
    }

    val openBracketToken = expectToken<Token.BracketOpen>("function call expression")
    skipTokens<Token.EOL>()
    val arguments = mutableListOf<Expression>()
    while (!isToken<Token.BracketClose>()) {
        val argument = parseExpression {
            parseUntilCondition(it) || it is Token.Comma || it is Token.BracketClose
        } ?: throw UnexpectedTokenException(
            expectedToken = "expression",
            actualToken = getToken(),
            currentlyParsing = "function call argument expression",
        )

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
        atToken = atToken,
        typeArgument = typeArgument,
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
    if ((isToken<Token.BracketOpen>() || isToken<Token.At>()) && lastAccessor is PropertyAccessAccessorExpression) {
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
            atToken = functionCallExpression.atToken,
            typeArgument = functionCallExpression.typeArgument,
            arguments = listOf(receiverExpressionArgument).plus(functionCallExpression.arguments),
            isMemberFunctionCall = true,
        )
    }

    return null
}
