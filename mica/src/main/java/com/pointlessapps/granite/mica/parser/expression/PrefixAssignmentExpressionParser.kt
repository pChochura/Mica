package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.PrefixAssignmentExpression
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parsePrefixAssignmentExpression(
    parseUntilCondition: (Token) -> Boolean,
): PrefixAssignmentExpression {
    val prefixOperatorToken = expectToken<Token>("prefix assignment expression") {
        it is Token.Increment || it is Token.Decrement
    }
    val symbolToken = expectToken<Token.Symbol>("prefix assignment expression") {
        it !is Token.Keyword
    }

    val accessorExpressions = parseAccessorExpressions(parseUntilCondition)
    return PrefixAssignmentExpression(prefixOperatorToken, symbolToken, accessorExpressions)
}
