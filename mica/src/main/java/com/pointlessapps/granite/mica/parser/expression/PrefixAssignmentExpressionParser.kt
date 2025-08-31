package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.ArrayIndex
import com.pointlessapps.granite.mica.ast.expressions.PrefixAssignmentExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parsePrefixAssignmentExpression(): PrefixAssignmentExpression {
    val prefixOperatorToken = expectToken<Token>("prefix assignment expression") {
        it is Token.Increment || it is Token.Decrement
    }
    val symbolToken = expectToken<Token.Symbol>("prefix assignment expression") {
        it !is Token.Keyword
    }
    val indexExpressions = mutableListOf<ArrayIndex>()
    while (isToken<Token.SquareBracketOpen>()) {
        val openBracketToken = expectToken<Token.SquareBracketOpen>("prefix assignment expression")
        val expression = parseExpression(0f) { it is Token.SquareBracketClose }
            ?: throw UnexpectedTokenException(
                expectedToken = "expression",
                actualToken = getToken(),
                currentlyParsing = "prefix assignment expression",
            )
        val closeBracketToken =
            expectToken<Token.SquareBracketClose>("prefix assignment expression")
        indexExpressions.add(
            ArrayIndex(openBracketToken, closeBracketToken, expression),
        )
    }

    return PrefixAssignmentExpression(prefixOperatorToken, symbolToken, indexExpressions)
}
