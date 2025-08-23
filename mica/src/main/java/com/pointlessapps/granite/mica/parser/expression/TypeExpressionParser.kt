package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseTypeExpression(
    parseUntilCondition: (Token) -> Boolean,
): TypeExpression {
    if (isToken<Token.SquareBracketOpen>()) {
        // Parse as an array
        val openBracketToken = expectToken<Token.SquareBracketOpen>("type expression")
        val typeExpression = parseTypeExpression {
            parseUntilCondition(it) || it is Token.SquareBracketClose
        }
        val closeBracketToken = expectToken<Token.SquareBracketClose>("type expression")

        return ArrayTypeExpression(
            openBracketToken = openBracketToken,
            closeBracketToken = closeBracketToken,
            typeExpression = typeExpression,
        )
    }

    val symbolToken = expectToken<Token.Symbol>("type expression")
    return SymbolTypeExpression(symbolToken)
}
