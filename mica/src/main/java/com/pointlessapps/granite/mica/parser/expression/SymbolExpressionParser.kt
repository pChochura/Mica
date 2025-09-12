package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.MemberAccessExpression
import com.pointlessapps.granite.mica.ast.expressions.PostfixAssignmentExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseSymbolExpression(
    parseUntilCondition: (Token) -> Boolean,
): Expression {
    if (getToken().let { it is Token.Keyword && it.value == Keyword.IF.value }) {
        return parseIfConditionExpression(parseUntilCondition)
    }

    val symbolToken = expectToken<Token.Symbol>("symbol expression") { it !is Token.Keyword }
    if (isToken<Token.BracketOpen>() || isToken<Token.At>()) {
        return parseFunctionCallExpression(symbolToken, parseUntilCondition)
    }

    val accessorExpressions = parseAccessorExpressions(parseUntilCondition)
    val memberFunctionCallExpression = parseMemberFunctionCallExpression(
        lhs = SymbolExpression(symbolToken),
        accessorExpressions = accessorExpressions,
        parseUntilCondition = parseUntilCondition,
    )

    if (memberFunctionCallExpression != null) return memberFunctionCallExpression

    if (getToken().let { it is Token.Increment || it is Token.Decrement }) {
        val postfixOperatorToken = expectToken<Token>("postfix assignment expression") {
            it is Token.Increment || it is Token.Decrement
        }

        return PostfixAssignmentExpression(symbolToken, accessorExpressions, postfixOperatorToken)
    }

    val symbolExpression = SymbolExpression(symbolToken)
    return if (accessorExpressions.isNotEmpty()) {
        MemberAccessExpression(
            symbolExpression = symbolExpression,
            accessorExpressions = accessorExpressions,
        )
    } else {
        symbolExpression
    }
}
