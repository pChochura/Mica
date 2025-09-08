package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.PrefixAssignmentExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.statement.parseArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.parser.statement.parsePropertyAccessAccessorExpression

internal fun Parser.parsePrefixAssignmentExpression(
    parseUntilCondition: (Token) -> Boolean,
): PrefixAssignmentExpression {
    val prefixOperatorToken = expectToken<Token>("prefix assignment expression") {
        it is Token.Increment || it is Token.Decrement
    }
    val symbolToken = expectToken<Token.Symbol>("prefix assignment expression") {
        it !is Token.Keyword
    }

    val accessorExpressions = mutableListOf<AccessorExpression>()
    while (isToken<Token.SquareBracketOpen>() || isToken<Token.Dot>()) {
        val accessorExpression = if (isToken<Token.SquareBracketOpen>()) {
            parseArrayIndexAccessorExpression(parseUntilCondition)
        } else if (isToken<Token.Dot>()) {
            parsePropertyAccessAccessorExpression()
        } else {
            throw UnexpectedTokenException(
                expectedToken = "array index or property access",
                actualToken = getToken(),
                currentlyParsing = "prefix assignment expression",
            )
        }

        accessorExpressions.add(accessorExpression)
    }

    return PrefixAssignmentExpression(prefixOperatorToken, symbolToken, accessorExpressions)
}
