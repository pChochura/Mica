package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.ast.ArrayIndexAccessorExpression
import com.pointlessapps.granite.mica.ast.MemberAccessAccessorExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression

internal fun Parser.parseAssignmentStatement(
    parseUntilCondition: (Token) -> Boolean,
): AssignmentStatement {
    val lhsToken = expectToken<Token.Symbol>("assignment statement") { it !is Token.Keyword }

    val accessorExpressions = mutableListOf<AccessorExpression>()
    while (isToken<Token.SquareBracketOpen>() || isToken<Token.Dot>()) {
        val accessorExpression = if (isToken<Token.SquareBracketOpen>()) {
            parseArrayIndexAccessorExpression(parseUntilCondition)
        } else if (isToken<Token.Dot>()) {
            parseMemberAccessAccessorExpression()
        } else {
            throw UnexpectedTokenException(
                expectedToken = "array index or member access",
                actualToken = getToken(),
                currentlyParsing = "assignment statement",
            )
        }

        accessorExpressions.add(accessorExpression)
    }

    val equalSignToken = expectToken<Token>("assignment statement") {
        it is Token.Equals || it is Token.PlusEquals || it is Token.MinusEquals
    }
    val rhs = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken(), "assignment statement")

    return AssignmentStatement(lhsToken, accessorExpressions, equalSignToken, rhs)
}

internal fun Parser.parseArrayIndexAccessorExpression(
    parseUntilCondition: (Token) -> Boolean,
): ArrayIndexAccessorExpression {
    val openBracketToken = expectToken<Token.SquareBracketOpen>("array index assignment statement")
    val expression = parseExpression(0f) {
        parseUntilCondition(it) || it is Token.SquareBracketClose
    } ?: throw UnexpectedTokenException(
        expectedToken = "expression",
        actualToken = getToken(),
        currentlyParsing = "array index assignment statement",
    )
    val closeBracketToken =
        expectToken<Token.SquareBracketClose>("array index assignment statement")

    return ArrayIndexAccessorExpression(openBracketToken, closeBracketToken, expression)
}

internal fun Parser.parseMemberAccessAccessorExpression(): MemberAccessAccessorExpression {
    val dotToken = expectToken<Token.Dot>("member access assignment statement")
    val propertySymbolToken = expectToken<Token.Symbol>("member access assignment statement") {
        it !is Token.Keyword
    }

    return MemberAccessAccessorExpression(dotToken, propertySymbolToken)
}
