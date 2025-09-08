package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression
import com.pointlessapps.granite.mica.parser.isAssignmentStatementStarting
import com.pointlessapps.granite.mica.parser.isFunctionDeclarationStatementStarting
import com.pointlessapps.granite.mica.parser.isVariableDeclarationStatementStarting

internal fun Parser.parseListOfStatements(
    parseUntilCondition: (Token) -> Boolean,
): List<Statement> {
    val statements = mutableListOf<Statement>()

    do {
        skipTokens<Token.EOL>()
        if (parseUntilCondition(getToken())) {
            break
        }

        val statement = parseStatement {
            parseUntilCondition(it) || it is Token.EOL || it is Token.EOF
        } ?: throw UnexpectedTokenException("statement", getToken(), "statement")

        statements.add(statement)
    } while (!parseUntilCondition(getToken()))

    return statements.toList()
}

internal fun Parser.parseStatement(
    parseUntilCondition: (Token) -> Boolean,
): Statement? {
    val savedIndex = currentIndex
    val statement = when (val token = getToken()) {
        is Token.Operator -> when (token.type) {
            Token.Operator.Type.GraterThan -> parseUserOutputCallStatement(parseUntilCondition)
            Token.Operator.Type.LessThan -> parseUserInputCallStatement()
            else -> parseExpression(0f, parseUntilCondition)?.let(::ExpressionStatement)
        }

        is Token.Keyword -> when (token.value) {
            Keyword.RETURN.value -> parseReturnStatement(parseUntilCondition)
            Keyword.BREAK.value -> parseBreakStatement()
            Keyword.LOOP.value -> parseLoopStatement(parseUntilCondition)
            Keyword.TYPE.value -> parseTypeDeclarationStatement()
            else -> parseExpression(0f, parseUntilCondition)?.let(::ExpressionStatement)
        }

        is Token.Symbol -> when {
            isVariableDeclarationStatementStarting() ->
                parseVariableDeclarationStatement(parseUntilCondition)

            isAssignmentStatementStarting() ->
                parseAssignmentStatement(parseUntilCondition)

            isFunctionDeclarationStatementStarting() ->
                parseFunctionDeclarationStatement()

            else -> parseExpression(0f, parseUntilCondition)?.let(::ExpressionStatement)
        }

        else -> parseExpression(0f, parseUntilCondition)?.let(::ExpressionStatement)
            ?: throw UnexpectedTokenException("statement", token, "statement")
    }

    if (statement != null) {
        return statement
    }

    restoreTo(savedIndex)
    return parseExpression(0f, parseUntilCondition)?.let(::ExpressionStatement)
}
