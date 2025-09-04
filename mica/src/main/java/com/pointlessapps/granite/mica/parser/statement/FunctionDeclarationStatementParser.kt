package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression
import com.pointlessapps.granite.mica.parser.expression.parseTypeExpression

internal fun Parser.parseFunctionDeclarationStatement(): FunctionDeclarationStatement {
    val nameToken = expectToken<Token.Symbol>("function declaration statement") {
        it !is Token.Keyword
    }
    val openBracketToken = expectToken<Token.BracketOpen>("function declaration statement")
    val parameters = parseFunctionParameterDeclarationStatements()
    val closeBracketToken = expectToken<Token.BracketClose>("function declaration statement")

    var colonToken: Token.Colon? = null
    var returnTypeExpression: TypeExpression? = null
    if (isToken<Token.Colon>()) {
        colonToken = expectToken<Token.Colon>("function declaration statement")
        returnTypeExpression = parseTypeExpression {
            it is Token.EOL || it is Token.CurlyBracketOpen
        }
    }

    skipTokens<Token.EOL>()
    val openCurlyToken = expectToken<Token.CurlyBracketOpen>("function declaration statement")
    val body = parseListOfStatements { it is Token.CurlyBracketClose }
    val closeCurlyToken = expectToken<Token.CurlyBracketClose>("function declaration statement")

    expectEOForEOL("function declaration statement")

    return FunctionDeclarationStatement(
        nameToken = nameToken,
        openBracketToken = openBracketToken,
        closeBracketToken = closeBracketToken,
        openCurlyToken = openCurlyToken,
        closeCurlyToken = closeCurlyToken,
        colonToken = colonToken,
        returnTypeExpression = returnTypeExpression,
        parameters = parameters,
        body = body,
    )
}

private fun Parser.parseFunctionParameterDeclarationStatements(): List<FunctionParameterDeclarationStatement> {
    skipTokens<Token.EOL>()
    val parameters = mutableListOf<FunctionParameterDeclarationStatement>()
    while (!isToken<Token.BracketClose>()) {
        val parameterNameToken = expectToken<Token.Symbol>("function parameter declaration") {
            it !is Token.Keyword
        }
        val parameterColonToken = expectToken<Token.Colon>("function parameter declaration")
        val parameterTypeExpression = parseTypeExpression {
            it is Token.Comma || it is Token.BracketClose
        }

        var equalsToken: Token.Equals? = null
        var defaultValueExpression: Expression? = null

        if (isToken<Token.Equals>()) {
            equalsToken = expectToken<Token.Equals>("function parameter default value")
            defaultValueExpression = parseExpression {
                it is Token.Comma || it is Token.BracketClose || it is Token.EOL
            } ?: throw UnexpectedTokenException(
                expectedToken = "expression",
                actualToken = getToken(),
                currentlyParsing = "function parameter default value",
            )
        }

        parameters.add(
            FunctionParameterDeclarationStatement(
                nameToken = parameterNameToken,
                colonToken = parameterColonToken,
                typeExpression = parameterTypeExpression,
                equalsToken = equalsToken,
                defaultValueExpression = defaultValueExpression,
            ),
        )

        skipTokens<Token.EOL>()
        if (isToken<Token.Comma>()) advance()
        skipTokens<Token.EOL>()
    }

    return parameters.toList()
}
