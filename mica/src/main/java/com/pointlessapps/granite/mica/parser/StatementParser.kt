package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.CommentStatement
import com.pointlessapps.granite.mica.ast.statements.ElseIfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.ElseStatement
import com.pointlessapps.granite.mica.ast.statements.EmptyStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.errors.MissingTokenException
import com.pointlessapps.granite.mica.errors.UnexpectedRootLevelInputException
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token

internal fun Parser.parseListOfStatements(
    parseUntilCondition: (Token) -> Boolean,
): List<Statement> {
    val statements = mutableListOf<Statement>()

    do {
        while (getToken() == Token.EOL) {
            advance()
        }

        if (!parseUntilCondition(getToken())) {
            break
        }

        val statement = parseStatement()
        if (statement == null) {
            throw UnexpectedRootLevelInputException("Only statements are allowed on the root level")
        }

        statements.add(statement)
    } while (parseUntilCondition(getToken()))

    return statements.toList()
}

private fun Parser.parseStatement(): Statement? {
    val statement = runCatching {
        when (val token = getToken()) {
            is Token.Whitespace -> EmptyStatement
            is Token.Comment -> parseCommentStatement()
            is Token.Operator -> when (token.type) {
                Token.Operator.Type.GraterThan -> parseUserOutputCallStatement()
                Token.Operator.Type.LessThan -> parseUserInputCallStatement()
                else -> throw UnexpectedTokenException("Expected statement, but got $token")
            }

            is Token.Symbol -> parseInSequence(
                ::parseFunctionDeclarationStatement,
                ::parseFunctionCallStatement,
                ::parseAssignmentStatement,
            )

            is Token.Keyword -> parseInSequence(
                ::parseIfConditionStatement,
                ::parseReturnStatement,
            )

            else -> throw UnexpectedTokenException("Expected statement, but got $token")
        }
    }.getOrNull()

    return statement ?: parseExpression()?.let(::ExpressionStatement)
}

private fun Parser.parseCommentStatement(): CommentStatement {
    val currentToken = expectToken<Token.Comment>()
    expectEOForEOL()

    return CommentStatement(currentToken)
}

// TODO support more types to be output
private fun Parser.parseUserOutputCallStatement(): UserOutputCallStatement {
    val userOutputStartingToken = expectToken<Token.Operator> {
        it.type == Token.Operator.Type.GraterThan
    }
    val stringLiteralToken = expectToken<Token.StringLiteral>()
    expectEOForEOL()

    return UserOutputCallStatement(userOutputStartingToken, stringLiteralToken)
}

private fun Parser.parseUserInputCallStatement(): UserInputCallStatement {
    val userInputStartingToken = expectToken<Token.Operator> {
        it.type == Token.Operator.Type.LessThan
    }
    val stringLiteralToken = expectToken<Token.Symbol>()
    expectEOForEOL()

    return UserInputCallStatement(userInputStartingToken, stringLiteralToken)
}

private fun Parser.parseFunctionDeclarationStatement(): FunctionDeclarationStatement {
    val nameToken = expectToken<Token.Symbol>()
    val openBracketToken = expectToken<Token.BracketOpen>()
    val parameters = parseFunctionParameterDeclarationStatements()
    val closeBracketToken = expectToken<Token.BracketClose>()

    var colonToken: Token.Colon? = null
    // TODO support custom return types
    var returnTypeToken: Token.Keyword? = null
    if (getToken() == Token.Colon) {
        colonToken = expectToken<Token.Colon>()
        returnTypeToken = expectToken<Token.Keyword>()
    }

    val openCurlyToken = expectToken<Token.CurlyBracketOpen>()
    val body = parseListOfStatements(parseUntilCondition = { it != Token.CurlyBracketClose })
    val closeCurlyToken = expectToken<Token.CurlyBracketClose>()

    return FunctionDeclarationStatement(
        nameToken = nameToken,
        openBracketToken = openBracketToken,
        closeBracketToken = closeBracketToken,
        openCurlyToken = openCurlyToken,
        closeCurlyToken = closeCurlyToken,
        colonToken = colonToken,
        returnTypeToken = returnTypeToken,
        parameters = parameters,
        body = body,
    )
}

private fun Parser.parseFunctionParameterDeclarationStatements(): List<FunctionParameterDeclarationStatement> {
    val parameters = mutableListOf<FunctionParameterDeclarationStatement>()
    while (getToken() != Token.BracketClose) {
        val parameterNameToken = expectToken<Token.Symbol>()
        val parameterColonToken = expectToken<Token.Colon>()
        val parameterTypeToken = expectToken<Token.Keyword>()

        parameters.add(
            FunctionParameterDeclarationStatement(
                nameToken = parameterNameToken,
                colonToken = parameterColonToken,
                typeToken = parameterTypeToken,
            ),
        )

        if (getToken() == Token.Comma) {
            advance()

            assert(getToken() != Token.BracketClose) {
                throw UnexpectedTokenException(
                    "Expected a parameter declaration, but got ${getToken()}",
                )
            }
        }
    }

    return parameters.toList()
}

private fun Parser.parseFunctionCallStatement(): FunctionCallStatement {
    val functionCallExpression = parseFunctionCallExpression()
    expectEOForEOL()

    return FunctionCallStatement(functionCallExpression)
}

private fun Parser.parseAssignmentStatement(): AssignmentStatement {
    val lhsToken = expectToken<Token.Symbol>()
    val equalSignToken = expectToken<Token.Equals>()
    val rhs = parseExpression()
        ?: throw MissingTokenException("Expected expression after $equalSignToken")
    expectEOForEOL()

    return AssignmentStatement(lhsToken, equalSignToken, rhs)
}

private fun Parser.parseIfConditionStatement(): IfConditionStatement {
    val ifToken = expectToken<Token.Keyword> { it.value == "if" }
    val expression = parseExpression(parseUntilCondition = { it == Token.CurlyBracketOpen })
        ?: throw UnexpectedTokenException("Expected expression, but got ${getToken()}")

    val openCurlyToken = expectToken<Token.CurlyBracketOpen>()
    val body = parseListOfStatements(parseUntilCondition = { it != Token.CurlyBracketClose })
    val closeCurlyToken = expectToken<Token.CurlyBracketClose>()

    if (getToken().let { it !is Token.Keyword || it.value != "else" }) {
        expectEOForEOL()

        return IfConditionStatement(
            ifToken = ifToken,
            conditionExpression = expression,
            openCurlyToken = openCurlyToken,
            closeCurlyToken = closeCurlyToken,
            body = body,
            elseIfConditionStatements = null,
            elseStatement = null,
        )
    }

    val elseIfConditionStatements = parseElseIfConditionStatements()
    val elseStatement = parseElseStatement()

    return IfConditionStatement(
        ifToken = ifToken,
        conditionExpression = expression,
        openCurlyToken = openCurlyToken,
        closeCurlyToken = closeCurlyToken,
        body = body,
        elseIfConditionStatements = elseIfConditionStatements.takeIf { it.isNotEmpty() },
        elseStatement = elseStatement,
    )
}

private fun Parser.parseElseIfConditionStatements(): List<ElseIfConditionStatement> {
    val elseIfConditionStatements = mutableListOf<ElseIfConditionStatement>()
    while (getToken().let { it is Token.Keyword && it.value == "else" }) {
        save()
        val elseToken = expectToken<Token.Keyword> { it.value == "else" }
        if (getToken().let { it !is Token.Keyword || it.value != "if" }) {
            restore()

            break
        }

        val elseIfToken = expectToken<Token.Keyword> { it.value == "if" }
        val elseIfExpression =
            parseExpression(parseUntilCondition = { it == Token.CurlyBracketOpen })
                ?: throw UnexpectedTokenException("Expected expression, but got ${getToken()}")
        val elseIfOpenCurlyToken = expectToken<Token.CurlyBracketOpen>()
        val elseIfBody =
            parseListOfStatements(parseUntilCondition = { it != Token.CurlyBracketClose })
        val elseIfCloseCurlyToken = expectToken<Token.CurlyBracketClose>()
        elseIfConditionStatements.add(
            ElseIfConditionStatement(
                elseIfToken = requireNotNull(elseToken) to elseIfToken,
                elseIfConditionExpression = elseIfExpression,
                elseIfOpenCurlyToken = elseIfOpenCurlyToken,
                elseIfCloseCurlyToken = elseIfCloseCurlyToken,
                elseIfBody = elseIfBody,
            ),
        )
    }

    return elseIfConditionStatements.toList()
}

private fun Parser.parseElseStatement(): ElseStatement? {
    if (getToken().let { it !is Token.Keyword || it.value != "else" }) {
        return null
    }

    val elseToken = expectToken<Token.Keyword> { it.value == "else" }
    val elseOpenCurlyToken = expectToken<Token.CurlyBracketOpen>()
    val elseBody = parseListOfStatements(parseUntilCondition = { it != Token.CurlyBracketClose })
    val elseCloseCurlyToken = expectToken<Token.CurlyBracketClose>()
    expectEOForEOL()

    return ElseStatement(
        elseToken = elseToken,
        elseOpenCurlyToken = elseOpenCurlyToken,
        elseCloseCurlyToken = elseCloseCurlyToken,
        elseBody = elseBody,
    )
}

private fun Parser.parseReturnStatement(): ReturnStatement {
    val returnToken = expectToken<Token.Keyword> { it.value == "return" }
    if (getToken().let { it == Token.EOL || it == Token.EOF }) {
        expectEOForEOL()
        return ReturnStatement(returnToken, null)
    }

    val returnValue = parseExpression()
        ?: throw UnexpectedTokenException("Expected expression after $returnToken")
    expectEOForEOL()

    return ReturnStatement(returnToken, returnValue)
}
