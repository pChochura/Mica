package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.ast.ArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.ast.PropertyAccessAccessorExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseAccessorExpressions(
    parseUntilCondition: (Token) -> Boolean,
): List<AccessorExpression> {
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
                currentlyParsing = "accessor expression",
            )
        }

        accessorExpressions.add(accessorExpression)
    }

    return accessorExpressions
}

private fun Parser.parseArrayIndexAccessorExpression(
    parseUntilCondition: (Token) -> Boolean,
): ArrayIndexAccessorExpression {
    val openBracketToken = expectToken<Token.SquareBracketOpen>("array index accessor expression")
    val expression = parseExpression(0f) {
        parseUntilCondition(it) || it is Token.SquareBracketClose
    } ?: throw UnexpectedTokenException(
        expectedToken = "expression",
        actualToken = getToken(),
        currentlyParsing = "array index accessor expression",
    )
    val closeBracketToken =
        expectToken<Token.SquareBracketClose>("array index accessor expression")

    return ArrayIndexAccessorExpression(openBracketToken, closeBracketToken, expression)
}

private fun Parser.parsePropertyAccessAccessorExpression(): PropertyAccessAccessorExpression {
    val dotToken = expectToken<Token.Dot>("property access accessor expression")
    val propertySymbolToken = expectToken<Token.Symbol>("property access accessor expression") {
        it !is Token.Keyword
    }

    return PropertyAccessAccessorExpression(dotToken, propertySymbolToken)
}
