package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseAccessorExpressions
import com.pointlessapps.granite.mica.parser.expression.parseExpression
import com.pointlessapps.granite.mica.parser.expression.parseMemberFunctionCallExpression
import com.pointlessapps.granite.mica.parser.expression.parseTypeExpression
import com.pointlessapps.granite.mica.parser.isFunctionDeclarationStatementStarting

internal fun Parser.parseSymbolStatement(
    parseUntilCondition: (Token) -> Boolean,
): Statement? {
    val savedIndex = currentIndex
    val symbolToken = expectToken<Token.Symbol>("symbol statement") { it !is Token.Keyword }
    if (isToken<Token.Colon>()) {
        return parseVariableDeclarationStatement(symbolToken)
    } else if (isFunctionDeclarationStatementStarting()) {
        return parseFunctionDeclarationStatement(symbolToken)
    }

    val accessorExpressions = parseAccessorExpressions(parseUntilCondition)
    val memberFunctionCallExpression = parseMemberFunctionCallExpression(
        lhs = SymbolExpression(symbolToken),
        accessorExpressions = accessorExpressions,
        parseUntilCondition = parseUntilCondition,
    )

    if (memberFunctionCallExpression != null) {
        return ExpressionStatement(memberFunctionCallExpression)
    }

    if (getToken().let { it is Token.Equals || it is Token.AssignmentOperator }) {
        val equalSignToken = expectToken<Token>("assignment statement") {
            it is Token.Equals || it is Token.AssignmentOperator
        }
        val rhs = parseExpression(0f) { it is Token.EOL || it is Token.EOF }
            ?: throw UnexpectedTokenException("expression", getToken(), "assignment statement")

        return AssignmentStatement(symbolToken, accessorExpressions, equalSignToken, rhs)
    }

    restoreTo(savedIndex)
    return parseExpression(0f, parseUntilCondition)?.let(::ExpressionStatement)
}

private fun Parser.parseVariableDeclarationStatement(
    symbolToken: Token.Symbol,
): VariableDeclarationStatement {
    val colonToken = expectToken<Token.Colon>("variable declaration statement")
    val typeExpression = parseTypeExpression { it is Token.Equals }
    val equalSignToken = expectToken<Token.Equals>("variable declaration statement")
    val rhs = parseExpression(0f) { it is Token.EOL || it is Token.EOF }
        ?: throw UnexpectedTokenException(
            expectedToken = "expression",
            actualToken = getToken(),
            currentlyParsing = "variable declaration statement",
        )

    return VariableDeclarationStatement(
        lhsToken = symbolToken,
        colonToken = colonToken,
        typeExpression = typeExpression,
        equalSignToken = equalSignToken,
        rhs = rhs,
    )
}
