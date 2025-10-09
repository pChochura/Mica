package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.ConstructorCallExpression
import com.pointlessapps.granite.mica.ast.expressions.PropertyValuePair
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseConstructorCallExpression(
    symbolToken: Token.Symbol,
    parseUntilCondition: (Token) -> Boolean,
): ConstructorCallExpression {
    val openBracketToken = expectToken<Token.CurlyBracketOpen>("constructor call expression")
    skipTokens<Token.EOL>()
    val propertyValuePairs = mutableListOf<PropertyValuePair>()
    while (!isToken<Token.CurlyBracketClose>()) {
        val propertyName = expectToken<Token.Symbol>("constructor call property expression")
        val equalsToken = expectToken<Token.Equals>("constructor call property expression")
        val valueExpression = parseExpression {
            parseUntilCondition(it) || it is Token.Comma || it is Token.CurlyBracketClose
        } ?: throw UnexpectedTokenException(
            expectedToken = "expression",
            actualToken = getToken(),
            currentlyParsing = "constructor call property expression",
        )

        propertyValuePairs.add(
            PropertyValuePair(
                propertyName = propertyName,
                equalsToken = equalsToken,
                valueExpression = valueExpression,
            )
        )

        skipTokens<Token.EOL>()
        if (isToken<Token.Comma>()) advance()
        skipTokens<Token.EOL>()
    }
    val closeBracketToken = expectToken<Token.CurlyBracketClose>("constructor call expression")

    return ConstructorCallExpression(
        nameToken = symbolToken,
        openCurlyBracketToken = openBracketToken,
        closeCurlyBracketToken = closeBracketToken,
        propertyValuePairs = propertyValuePairs,
    )
}
