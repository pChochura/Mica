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
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token

internal fun Parser.parseListOfStatements(
    parseUntilCondition: (Token) -> Boolean,
): List<Statement> {
    val statements = mutableListOf<Statement>()

    do {
        while (isToken<Token.EOL>()) {
            advance()
        }

        if (!parseUntilCondition(getToken())) {
            break
        }

        val statement = parseStatement() ?: throw UnexpectedTokenException("statement", getToken())

        statements.add(statement)
    } while (parseUntilCondition(getToken()))

    return statements.toList()
}

private fun Parser.parseStatement(): Statement? {
    val savedIndex = currentIndex
    val statement = runCatching {
        when (val token = getToken()) {
            is Token.Whitespace -> EmptyStatement(token)
            is Token.Comment -> parseCommentStatement()
            is Token.Operator -> when (token.type) {
                Token.Operator.Type.GraterThan -> parseUserOutputCallStatement()
                Token.Operator.Type.LessThan -> parseUserInputCallStatement()
                else -> throw UnexpectedTokenException("> or <", token)
            }

            is Token.Symbol -> parseInSequence(
                ::parseReturnStatement,
                ::parseIfConditionStatement,
                ::parseFunctionDeclarationStatement,
                ::parseFunctionCallStatement,
                ::parseVariableDeclarationStatement,
                ::parseAssignmentStatement,
            )

            else -> throw UnexpectedTokenException("statement", token)
        }
    }.getOrNull()

    if (statement != null) {
        return statement
    }

    restoreTo(savedIndex)
    return parseExpression()?.let(::ExpressionStatement)
}

private fun Parser.parseCommentStatement(): CommentStatement {
    val currentToken = expectToken<Token.Comment>()
    expectEOForEOL()

    return CommentStatement(currentToken)
}

private fun Parser.parseUserOutputCallStatement(): UserOutputCallStatement {
    val userOutputStartingToken = expectToken<Token.Operator> {
        it.type == Token.Operator.Type.GraterThan
    }
    val expression = parseExpression() ?: throw UnexpectedTokenException("expression", getToken())
    expectEOForEOL()

    return UserOutputCallStatement(userOutputStartingToken, expression)
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
    var returnTypeToken: Token.Symbol? = null
    if (isToken<Token.Colon>()) {
        colonToken = expectToken<Token.Colon>()
        returnTypeToken = expectToken<Token.Symbol>()
    }

    val openCurlyToken = expectToken<Token.CurlyBracketOpen>()
    val body = parseListOfStatements(parseUntilCondition = { it !is Token.CurlyBracketClose })
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
    while (!isToken<Token.BracketClose>()) {
        val parameterNameToken = expectToken<Token.Symbol>()
        val parameterColonToken = expectToken<Token.Colon>()
        val parameterTypeToken = expectToken<Token.Symbol>()

        parameters.add(
            FunctionParameterDeclarationStatement(
                nameToken = parameterNameToken,
                colonToken = parameterColonToken,
                typeToken = parameterTypeToken,
            ),
        )

        if (isToken<Token.Comma>()) {
            advance()

            assert(!isToken<Token.BracketClose>()) {
                throw UnexpectedTokenException("parameter declaration", getToken())
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

private fun Parser.parseVariableDeclarationStatement(): VariableDeclarationStatement {
    val lhsToken = expectToken<Token.Symbol>()
    val colonToken = expectToken<Token.Colon>()
    val typeToken = expectToken<Token.Symbol>()
    val equalSignToken = expectToken<Token.Equals>()
    val rhs = parseExpression() ?: throw UnexpectedTokenException("expression", getToken())
    expectEOForEOL()

    return VariableDeclarationStatement(lhsToken, colonToken, typeToken, equalSignToken, rhs)
}

private fun Parser.parseAssignmentStatement(): AssignmentStatement {
    val lhsToken = expectToken<Token.Symbol>()
    val equalSignToken = expectToken<Token.Equals>()
    val rhs = parseExpression() ?: throw UnexpectedTokenException("expression", getToken())
    expectEOForEOL()

    return AssignmentStatement(lhsToken, equalSignToken, rhs)
}

private fun Parser.parseIfConditionStatement(): IfConditionStatement {
    val ifToken = expectToken<Token.Keyword> { it.value == "if" }
    val expression = parseExpression(
        parseUntilCondition = { it is Token.CurlyBracketOpen || it is Token.EOL },
    ) ?: throw UnexpectedTokenException("expression", getToken())

    // Parse as a one line if statement
    var body: List<Statement>
    var openCurlyToken: Token.CurlyBracketOpen? = null
    var closeCurlyToken: Token.CurlyBracketClose? = null
    if (!isToken<Token.CurlyBracketOpen>()) {
        expectToken<Token.EOL>()

        body = listOf(parseStatement() ?: throw UnexpectedTokenException("statement", getToken()))
    } else {
        openCurlyToken = expectToken<Token.CurlyBracketOpen>()
        body = parseListOfStatements(parseUntilCondition = { it !is Token.CurlyBracketClose })
        closeCurlyToken = expectToken<Token.CurlyBracketClose>()
    }

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
        val savedIndex = currentIndex
        val elseToken = expectToken<Token.Keyword> { it.value == "else" }
        if (getToken().let { it !is Token.Keyword || it.value != "if" }) {
            restoreTo(savedIndex)

            break
        }

        val elseIfToken = expectToken<Token.Keyword> { it.value == "if" }
        val elseIfExpression = parseExpression(
            parseUntilCondition = { it is Token.CurlyBracketOpen || it is Token.EOL },
        ) ?: throw UnexpectedTokenException("expression", getToken())

        // Parse as a one line if statement
        var elseIfBody: List<Statement>
        var elseIfOpenCurlyToken: Token.CurlyBracketOpen? = null
        var elseIfCloseCurlyToken: Token.CurlyBracketClose? = null
        if (!isToken<Token.CurlyBracketOpen>()) {
            expectToken<Token.EOL>()

            elseIfBody = listOf(
                parseStatement() ?: throw UnexpectedTokenException("statement", getToken()),
            )
        } else {
            elseIfOpenCurlyToken = expectToken<Token.CurlyBracketOpen>()
            elseIfBody =
                parseListOfStatements(parseUntilCondition = { it !is Token.CurlyBracketClose })
            elseIfCloseCurlyToken = expectToken<Token.CurlyBracketClose>()
        }

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

    // Parse as a one line if statement
    var elseBody: List<Statement>
    var elseOpenCurlyToken: Token.CurlyBracketOpen? = null
    var elseCloseCurlyToken: Token.CurlyBracketClose? = null
    if (!isToken<Token.CurlyBracketOpen>()) {
        if (isToken<Token.EOL>()) advance()

        elseBody = listOf(
            parseStatement() ?: throw UnexpectedTokenException("statement", getToken()),
        )
    } else {
        elseOpenCurlyToken = expectToken<Token.CurlyBracketOpen>()
        elseBody = parseListOfStatements(parseUntilCondition = { it !is Token.CurlyBracketClose })
        elseCloseCurlyToken = expectToken<Token.CurlyBracketClose>()
    }
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
    if (getToken().let { it is Token.EOL || it is Token.EOF }) {
        expectEOForEOL()
        return ReturnStatement(returnToken, null)
    }

    val returnValue = parseExpression() ?: throw UnexpectedTokenException("expression", getToken())
    expectEOForEOL()

    return ReturnStatement(returnToken, returnValue)
}
