package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.KeyValuePair
import com.pointlessapps.granite.mica.ast.expressions.MapLiteralExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseMapLiteralExpression(
    parseUntilCondition: (Token) -> Boolean,
): MapLiteralExpression {
    val openBracketToken = expectToken<Token.CurlyBracketOpen>("map literal expression")

    // Handle empty map
    if (isToken<Token.Colon>()) {
        expectToken<Token.Colon>("map literal expression")
        val closeBracketToken = expectToken<Token.CurlyBracketClose>("map literal expression")
        return MapLiteralExpression(openBracketToken, closeBracketToken, emptyList())
    }

    skipTokens<Token.EOL>()
    val keyValuePairs = mutableListOf<KeyValuePair>()
    while (!isToken<Token.CurlyBracketClose>()) {
        val keyExpression = parseExpression {
            parseUntilCondition(it) || it is Token.Colon
        } ?: throw UnexpectedTokenException("expression", getToken(), "map literal expression")
        val colonToken = expectToken<Token.Colon>("map literal expression")
        val valueExpression = parseExpression {
            parseUntilCondition(it) || it is Token.Comma || it is Token.CurlyBracketClose
        } ?: throw UnexpectedTokenException("expression", getToken(), "map literal expression")

        keyValuePairs.add(
            KeyValuePair(
                keyExpression = keyExpression,
                colonToken = colonToken,
                valueExpression = valueExpression,
            ),
        )

        skipTokens<Token.EOL>()
        if (isToken<Token.Comma>()) advance()
        skipTokens<Token.EOL>()
    }
    val closeBracketToken = expectToken<Token.CurlyBracketClose>("map literal expression")

    return MapLiteralExpression(openBracketToken, closeBracketToken, keyValuePairs)
}
