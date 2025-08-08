package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.parseExpression

internal fun Parser.parseUserOutputCallStatement(
    parseUntilCondition: (Token) -> Boolean,
): UserOutputCallStatement {
    val userOutputStartingToken = expectToken<Token.Operator> {
        it.type == Token.Operator.Type.GraterThan
    }
    val expression = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken())
    expectEOForEOL()

    return UserOutputCallStatement(userOutputStartingToken, expression)
}
