package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.InterpolatedStringExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseInterpolatedStringExpression(): Expression {
    expectToken<Token.InterpolatedStringQuote>("string")
    if (isToken<Token.InterpolatedStringQuote>()) {
        val quoteToken = expectToken<Token.InterpolatedStringQuote>("string")

        return StringLiteralExpression(
            Token.StringLiteral(
                location = quoteToken.location,
                value = "",
            ),
        )
    }

    val expressions = mutableListOf<Expression>()
    while (!isToken<Token.InterpolatedStringQuote>()) {
        if (isToken<Token.InterpolatedStringStart>()) {
            expectToken<Token.InterpolatedStringStart>("interpolated string")
            val expression = parseExpression {
                it is Token.InterpolatedStringEnd
            } ?: throw UnexpectedTokenException(
                expectedToken = "expression",
                actualToken = getToken(),
                currentlyParsing = "interpolated string",
            )
            expressions.add(expression)
            expectToken<Token.InterpolatedStringEnd>("interpolated string")
        } else {
            expressions.add(
                StringLiteralExpression(expectToken<Token.StringLiteral>("interpolated string")),
            )
        }
    }
    expectToken<Token.InterpolatedStringQuote>("string")

    return InterpolatedStringExpression(expressions)
}
