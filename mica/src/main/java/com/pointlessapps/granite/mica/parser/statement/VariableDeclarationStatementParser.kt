package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.parseExpression
import com.pointlessapps.granite.mica.parser.parseType

internal fun Parser.parseVariableDeclarationStatement(
    parseUntilCondition: (Token) -> Boolean,
): VariableDeclarationStatement {
    val lhsToken = expectToken<Token.Symbol>()
    val colonToken = expectToken<Token.Colon>()
    val typeExpression = parseType(
        parseUntilCondition = { parseUntilCondition(it) || it is Token.Equals },
    )
    val equalSignToken = expectToken<Token.Equals>()
    val rhs = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken())

    return VariableDeclarationStatement(lhsToken, colonToken, typeExpression, equalSignToken, rhs)
}
