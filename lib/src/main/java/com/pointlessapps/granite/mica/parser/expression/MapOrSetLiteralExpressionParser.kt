package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.KeyValuePair
import com.pointlessapps.granite.mica.ast.expressions.MapLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SetLiteralExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseMapOrSetLiteralExpression(
    parseUntilCondition: (Token) -> Boolean,
): Expression {
    val openBracketToken = expectToken<Token.CurlyBracketOpen>("map or set literal expression")

    // Handle empty map
    if (isToken<Token.Colon>()) {
        advance()
        val closeBracketToken = expectToken<Token.CurlyBracketClose>("map literal expression")
        return MapLiteralExpression(openBracketToken, closeBracketToken, emptyList())
    }

    val (keyValuePairs, elements) = parseMapOrSetEntries(parseUntilCondition)
    val closeBracketToken = expectToken<Token.CurlyBracketClose>("set literal expression")

    return if (keyValuePairs.isEmpty()) {
        SetLiteralExpression(openBracketToken, closeBracketToken, elements)
    } else {
        MapLiteralExpression(openBracketToken, closeBracketToken, keyValuePairs)
    }
}

private fun Parser.parseMapOrSetEntries(
    parseUntilCondition: (Token) -> Boolean,
): Pair<List<KeyValuePair>, List<Expression>> {
    val keyValuePairs = mutableListOf<KeyValuePair>()
    val elements = mutableListOf<Expression>()
    skipTokens<Token.EOL>()
    while (!isToken<Token.CurlyBracketClose>()) {
        val expression = parseExpression {
            parseUntilCondition(it) || it is Token.Colon || it is Token.Comma || it is Token.CurlyBracketClose
        } ?: throw UnexpectedTokenException(
            "expression",
            getToken(),
            "map or set literal expression",
        )

        if (isToken<Token.Colon>() && elements.isEmpty()) {
            val colonToken = expectToken<Token.Colon>("map literal expression")
            val valueExpression = parseExpression {
                parseUntilCondition(it) || it is Token.Comma || it is Token.CurlyBracketClose
            } ?: throw UnexpectedTokenException("expression", getToken(), "map literal expression")

            keyValuePairs.add(KeyValuePair(expression, colonToken, valueExpression))
        } else {
            if (!keyValuePairs.isEmpty()) {
                throw UnexpectedTokenException(":", getToken(), "map literal expression")
            }

            elements.add(expression)
        }

        skipTokens<Token.EOL>()
        if (isToken<Token.Comma>()) advance()
        skipTokens<Token.EOL>()
    }

    return keyValuePairs to elements
}
