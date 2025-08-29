package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.ArrayIndex
import com.pointlessapps.granite.mica.ast.statements.ArrayAssignmentStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression

internal fun Parser.parseArrayAssignmentStatement(
    parseUntilCondition: (Token) -> Boolean,
): ArrayAssignmentStatement {
    val arraySymbolToken = expectToken<Token.Symbol>("array assignment statement") {
        it !is Token.Keyword
    }
    val indexExpressions = mutableListOf<ArrayIndex>()

    while (isToken<Token.SquareBracketOpen>()) {
        val openBracketToken = expectToken<Token.SquareBracketOpen>("array assignment statement")
        val expression =
            parseExpression(0f) { parseUntilCondition(it) || it is Token.SquareBracketClose }
                ?: throw UnexpectedTokenException(
                    expectedToken = "expression",
                    actualToken = getToken(),
                    currentlyParsing = "array assignment statement",
                )
        val closeBracketToken = expectToken<Token.SquareBracketClose>("array assignment statement")
        indexExpressions.add(
            ArrayIndex(openBracketToken, closeBracketToken, expression),
        )
    }

    val equalSignToken = expectToken<Token>("array assignment statement") {
        it is Token.Equals || it is Token.PlusEquals || it is Token.MinusEquals
    }
    val rhs = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken(), "array assignment statement")

    return ArrayAssignmentStatement(arraySymbolToken, indexExpressions, equalSignToken, rhs)
}
