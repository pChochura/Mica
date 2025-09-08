package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.PostfixAssignmentExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.statement.parseArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.parser.statement.parseMemberAccessAccessorExpression

internal fun Parser.parsePostfixAssignmentExpression(
    parseUntilCondition: (Token) -> Boolean,
): PostfixAssignmentExpression {
    val symbolToken = expectToken<Token.Symbol>("postfix assignment expression") {
        it !is Token.Keyword
    }

    val accessorExpressions = mutableListOf<AccessorExpression>()
    while (isToken<Token.SquareBracketOpen>() || isToken<Token.Dot>()) {
        val accessorExpression = if (isToken<Token.SquareBracketOpen>()) {
            parseArrayIndexAccessorExpression(parseUntilCondition)
        } else if (isToken<Token.Dot>()) {
            parseMemberAccessAccessorExpression()
        } else {
            throw UnexpectedTokenException(
                expectedToken = "array index or member access",
                actualToken = getToken(),
                currentlyParsing = "postfix assignment expression",
            )
        }

        accessorExpressions.add(accessorExpression)
    }

    val postfixOperatorToken = expectToken<Token>("postfix assignment expression") {
        it is Token.Increment || it is Token.Decrement
    }

    return PostfixAssignmentExpression(symbolToken, accessorExpressions, postfixOperatorToken)
}
