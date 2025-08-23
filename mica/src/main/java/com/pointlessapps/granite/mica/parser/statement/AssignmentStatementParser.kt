package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression

internal fun Parser.parseAssignmentStatement(
    parseUntilCondition: (Token) -> Boolean,
): AssignmentStatement {
    val lhsToken = expectToken<Token.Symbol>("assignment statement") {
        it !is Token.Keyword
    }
    val equalSignToken = expectToken<Token>("assignment statement") {
        it is Token.Equals || it is Token.PlusEquals || it is Token.MinusEquals
    }
    val rhs = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken(), "assignment statement")

    return AssignmentStatement(lhsToken, equalSignToken, rhs)
}
