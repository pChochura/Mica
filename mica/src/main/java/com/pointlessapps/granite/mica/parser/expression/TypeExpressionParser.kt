package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.SetTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseTypeExpression(
    parseUntilCondition: (Token) -> Boolean,
): TypeExpression {
    if (isToken<Token.SquareBracketOpen>()) {
        // Parse as an array
        val openBracketToken = expectToken<Token.SquareBracketOpen>("array type expression")
        val typeExpression = parseTypeExpression {
            parseUntilCondition(it) || it is Token.SquareBracketClose
        }
        val closeBracketToken = expectToken<Token.SquareBracketClose>("array type expression")

        return ArrayTypeExpression(
            openBracketToken = openBracketToken,
            closeBracketToken = closeBracketToken,
            typeExpression = typeExpression,
        )
    } else if (isToken<Token.CurlyBracketOpen>()) {
        // Parse as a set
        val openBracketToken = expectToken<Token.CurlyBracketOpen>("set type expression")
        val typeExpression = parseTypeExpression {
            parseUntilCondition(it) || it is Token.CurlyBracketClose
        }
        val closeBracketToken = expectToken<Token.CurlyBracketClose>("set type expression")

        return SetTypeExpression(
            openBracketToken = openBracketToken,
            closeBracketToken = closeBracketToken,
            typeExpression = typeExpression,
        )
    }

    val symbolToken = expectToken<Token.Symbol>("type expression")
    return SymbolTypeExpression(symbolToken)
}
