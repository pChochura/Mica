package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.ArrayIndex
import com.pointlessapps.granite.mica.ast.expressions.PostfixAssignmentExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parsePostfixUnaryExpression(): PostfixAssignmentExpression {
    val symbolToken = expectToken<Token.Symbol>("postfix assignment expression") {
        it !is Token.Keyword
    }
    val indexExpressions = mutableListOf<ArrayIndex>()
    while (isToken<Token.SquareBracketOpen>()) {
        val openBracketToken = expectToken<Token.SquareBracketOpen>("postfix assignment expression")
        val expression = parseExpression(0f) { it is Token.SquareBracketClose }
            ?: throw UnexpectedTokenException(
                expectedToken = "expression",
                actualToken = getToken(),
                currentlyParsing = "postfix assignment expression",
            )
        val closeBracketToken =
            expectToken<Token.SquareBracketClose>("postfix assignment expression")
        indexExpressions.add(
            ArrayIndex(openBracketToken, closeBracketToken, expression),
        )
    }
    val postfixOperatorToken = expectToken<Token>("postfix assignment expression") {
        it is Token.Increment || it is Token.Decrement
    }

    return PostfixAssignmentExpression(symbolToken, indexExpressions, postfixOperatorToken)
}
