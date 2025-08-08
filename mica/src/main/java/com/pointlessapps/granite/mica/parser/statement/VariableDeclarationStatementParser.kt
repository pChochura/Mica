package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.parseExpression

internal fun Parser.parseVariableDeclarationStatement(
    parseUntilCondition: (Token) -> Boolean,
): VariableDeclarationStatement {
    val lhsToken = expectToken<Token.Symbol>()
    val colonToken = expectToken<Token.Colon>()
    val typeToken = expectToken<Token.Symbol>()
    val equalSignToken = expectToken<Token.Equals>()
    val rhs = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken())
    expectEOForEOL()

    return VariableDeclarationStatement(lhsToken, colonToken, typeToken, equalSignToken, rhs)
}
