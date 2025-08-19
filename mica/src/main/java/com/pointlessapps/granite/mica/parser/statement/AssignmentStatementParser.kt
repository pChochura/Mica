package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression

internal fun Parser.parseAssignmentStatement(
    parseUntilCondition: (Token) -> Boolean,
): AssignmentStatement {
    val lhsToken = expectToken<Token.Symbol>()
    val equalSignToken = expectToken<Token.Equals>()
    val rhs = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken())

    return AssignmentStatement(lhsToken, equalSignToken, rhs)
}
