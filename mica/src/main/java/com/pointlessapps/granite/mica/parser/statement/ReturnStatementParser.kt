package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.parseExpression

internal fun Parser.parseReturnStatement(
    parseUntilCondition: (Token) -> Boolean,
): ReturnStatement {
    val returnToken = expectToken<Token.Keyword> { it.value == Keyword.RETURN.value }
    if (getToken().let { it is Token.EOL || it is Token.EOF }) {
        expectEOForEOL()
        return ReturnStatement(returnToken, null)
    }

    val returnValue = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException("expression", getToken())

    return ReturnStatement(returnToken, returnValue)
}
