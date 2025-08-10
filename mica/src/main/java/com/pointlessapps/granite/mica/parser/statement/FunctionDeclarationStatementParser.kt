package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.parseType

internal fun Parser.parseFunctionDeclarationStatement(): FunctionDeclarationStatement {
    val nameToken = expectToken<Token.Symbol>()
    val openBracketToken = expectToken<Token.BracketOpen>()
    val parameters = parseFunctionParameterDeclarationStatements()
    val closeBracketToken = expectToken<Token.BracketClose>()

    var colonToken: Token.Colon? = null
    var returnTypeExpression: TypeExpression? = null
    if (isToken<Token.Colon>()) {
        colonToken = expectToken<Token.Colon>()
        returnTypeExpression = parseType(parseUntilCondition = { it is Token.EOL || it is Token.CurlyBracketOpen })
    }

    skipTokens<Token.EOL>()
    val openCurlyToken = expectToken<Token.CurlyBracketOpen>()
    val body = parseListOfStatements(parseUntilCondition = { it !is Token.CurlyBracketClose })
    val closeCurlyToken = expectToken<Token.CurlyBracketClose>()

    expectEOForEOL()

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

internal fun Parser.parseFunctionParameterDeclarationStatements(): List<FunctionParameterDeclarationStatement> {
    val parameters = mutableListOf<FunctionParameterDeclarationStatement>()
    while (!isToken<Token.BracketClose>()) {
        val parameterNameToken = expectToken<Token.Symbol>()
        val parameterColonToken = expectToken<Token.Colon>()
        val parameterTypeExpression = parseType(
            parseUntilCondition = { it is Token.Comma || it is Token.BracketClose },
        )

        parameters.add(
            FunctionParameterDeclarationStatement(
                nameToken = parameterNameToken,
                colonToken = parameterColonToken,
                typeExpression = parameterTypeExpression,
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
