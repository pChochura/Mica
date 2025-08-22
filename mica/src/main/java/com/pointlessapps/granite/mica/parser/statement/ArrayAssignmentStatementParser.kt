package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.expressions.ArrayAssignmentIndexExpression
import com.pointlessapps.granite.mica.ast.statements.ArrayAssignmentStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression

internal fun Parser.parseArrayAssignmentStatement(
    parseUntilCondition: (Token) -> Boolean,
): ArrayAssignmentStatement {
    val arraySymbolToken = expectToken<Token.Symbol>()
    val indexExpressions = mutableListOf<ArrayAssignmentIndexExpression>()

    while (isToken<Token.SquareBracketOpen>()) {
        val openBracketToken = expectToken<Token.SquareBracketOpen>()
        val expression = parseExpression(0f) { parseUntilCondition(it) || it is Token.SquareBracketClose }
            ?: throw UnexpectedTokenException("expression", getToken())
        val closeBracketToken = expectToken<Token.SquareBracketClose>()
        indexExpressions.add(
            ArrayAssignmentIndexExpression(openBracketToken, closeBracketToken, expression),
        )
    }

    val equalSignToken = expectToken<Token> {
        it is Token.Equals || it is Token.PlusEquals || it is Token.MinusEquals
    }
    val rhs = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken())

    return ArrayAssignmentStatement(arraySymbolToken, indexExpressions, equalSignToken, rhs)
}
