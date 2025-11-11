package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.MapTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.SetTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseTypeExpression(
    parseUntilCondition: (Token) -> Boolean,
): TypeExpression {
    if (isToken<Token.SquareBracketOpen>()) {
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
        val openBracketToken = expectToken<Token.CurlyBracketOpen>("map or set type expression")
        val typeExpression = parseTypeExpression {
            parseUntilCondition(it) || it is Token.Colon || it is Token.CurlyBracketClose
        }

        if (isToken<Token.Colon>()) {
            val colonToken = expectToken<Token.Colon>("map type expression")
            val valueTypeExpression = parseTypeExpression {
                parseUntilCondition(it) || it is Token.CurlyBracketClose
            }
            val closeBracketToken = expectToken<Token.CurlyBracketClose>("map type expression")

            return MapTypeExpression(
                openBracketToken = openBracketToken,
                closeBracketToken = closeBracketToken,
                colonToken = colonToken,
                keyTypeExpression = typeExpression,
                valueTypeExpression = valueTypeExpression,
            )
        }

        val closeBracketToken = expectToken<Token.CurlyBracketClose>("set type expression")
        return SetTypeExpression(
            openBracketToken = openBracketToken,
            closeBracketToken = closeBracketToken,
            typeExpression = typeExpression,
        )
    }

    val symbolToken = expectToken<Token.Symbol>("type expression")
    var atToken: Token.At? = null
    var typeParameterConstraint: TypeExpression? = null
    if (isToken<Token.At>()) {
        atToken = expectToken<Token.At>("type expression parameter constraint")
        typeParameterConstraint = parseTypeExpression(parseUntilCondition)
    }

    return SymbolTypeExpression(symbolToken, atToken, typeParameterConstraint)
}
