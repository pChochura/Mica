package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression
import com.pointlessapps.granite.mica.parser.expression.parseTypeExpression

internal fun Parser.parseVariableDeclarationStatement(
    parseUntilCondition: (Token) -> Boolean,
): VariableDeclarationStatement {
    val lhsToken = expectToken<Token.Symbol>("variable declaration statement") {
        it !is Token.Keyword
    }
    val colonToken = expectToken<Token.Colon>("variable declaration statement")
    val typeExpression = parseTypeExpression {
        parseUntilCondition(it) || it is Token.Equals
    }
    val equalSignToken = expectToken<Token.Equals>("variable declaration statement")
    val rhs = parseExpression(0f, parseUntilCondition)
        ?: throw UnexpectedTokenException(
            expectedToken = "expression",
            actualToken = getToken(),
            currentlyParsing = "variable declaration statement",
        )

    return VariableDeclarationStatement(lhsToken, colonToken, typeExpression, equalSignToken, rhs)
}
