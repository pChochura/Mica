package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.ElseDeclaration
import com.pointlessapps.granite.mica.ast.statements.ElseIfConditionDeclaration
import com.pointlessapps.granite.mica.ast.statements.IfConditionDeclaration
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression

internal fun Parser.parseIfConditionStatement(
    parseUntilCondition: (Token) -> Boolean,
): IfConditionStatement {
    val ifConditionDeclaration = parseIfConditionDeclaration(parseUntilCondition)
    val elseIfConditionDeclarations = parseElseIfConditionDeclarations(parseUntilCondition)
    val elseDeclaration = parseElseDeclaration(parseUntilCondition)

    return IfConditionStatement(
        ifConditionDeclaration = ifConditionDeclaration,
        elseIfConditionDeclarations = elseIfConditionDeclarations.takeIf { it.isNotEmpty() },
        elseDeclaration = elseDeclaration,
    )
}

internal fun Parser.parseIfConditionDeclaration(
    parseUntilCondition: (Token) -> Boolean,
): IfConditionDeclaration {
    val ifToken = expectToken<Token.Keyword> { it.value == Keyword.IF.value }
    val expression = parseExpression {
        parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
    } ?: throw UnexpectedTokenException("expression", getToken())

    // Parse as a one line if statement
    var body: List<Statement>
    var openCurlyToken: Token.CurlyBracketOpen? = null
    var closeCurlyToken: Token.CurlyBracketClose? = null

    skipTokens<Token.EOL>()

    if (!isToken<Token.CurlyBracketOpen>()) {
        body = listOf(
            parseStatement(parseUntilCondition)
                ?: throw UnexpectedTokenException("statement", getToken()),
        )
        if (isToken<Token.EOL>()) advance()
    } else {
        openCurlyToken = expectToken<Token.CurlyBracketOpen>()
        body = parseListOfStatements { it !is Token.CurlyBracketClose }
        closeCurlyToken = expectToken<Token.CurlyBracketClose>()
        skipTokens<Token.EOL>()
    }

    return IfConditionDeclaration(
        ifToken = ifToken,
        ifConditionExpression = expression,
        ifOpenCurlyToken = openCurlyToken,
        ifCloseCurlyToken = closeCurlyToken,
        ifBody = body,
    )
}

internal fun Parser.parseElseIfConditionDeclarations(
    parseUntilCondition: (Token) -> Boolean,
): List<ElseIfConditionDeclaration> {
    val elseIfConditionDeclarations = mutableListOf<ElseIfConditionDeclaration>()
    while (getToken().let { it is Token.Keyword && it.value == Keyword.ELSE.value }) {
        val savedIndex = currentIndex
        val elseToken = expectToken<Token.Keyword> { it.value == Keyword.ELSE.value }
        if (getToken().let { it !is Token.Keyword || it.value != Keyword.IF.value }) {
            restoreTo(savedIndex)

            break
        }

        val elseIfToken = expectToken<Token.Keyword> { it.value == Keyword.IF.value }
        val elseIfExpression = parseExpression {
            parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
        } ?: throw UnexpectedTokenException("expression", getToken())

        // Parse as a one line if statement
        var elseIfBody: List<Statement>
        var elseIfOpenCurlyToken: Token.CurlyBracketOpen? = null
        var elseIfCloseCurlyToken: Token.CurlyBracketClose? = null

        skipTokens<Token.EOL>()

        if (!isToken<Token.CurlyBracketOpen>()) {
            elseIfBody = listOf(
                parseStatement(parseUntilCondition)
                    ?: throw UnexpectedTokenException("statement", getToken()),
            )
            if (isToken<Token.EOL>()) advance()
        } else {
            elseIfOpenCurlyToken = expectToken<Token.CurlyBracketOpen>()
            elseIfBody = parseListOfStatements { it !is Token.CurlyBracketClose }
            elseIfCloseCurlyToken = expectToken<Token.CurlyBracketClose>()
            skipTokens<Token.EOL>()
        }

        elseIfConditionDeclarations.add(
            ElseIfConditionDeclaration(
                elseIfToken = requireNotNull(elseToken) to elseIfToken,
                elseIfConditionExpression = elseIfExpression,
                elseIfOpenCurlyToken = elseIfOpenCurlyToken,
                elseIfCloseCurlyToken = elseIfCloseCurlyToken,
                elseIfBody = elseIfBody,
            ),
        )
    }

    return elseIfConditionDeclarations.toList()
}

internal fun Parser.parseElseDeclaration(
    parseUntilCondition: (Token) -> Boolean,
): ElseDeclaration? {
    if (getToken().let { it !is Token.Keyword || it.value != Keyword.ELSE.value }) {
        return null
    }

    val elseToken = expectToken<Token.Keyword> { it.value == Keyword.ELSE.value }

    // Parse as a one line if statement
    var elseBody: List<Statement>
    var elseOpenCurlyToken: Token.CurlyBracketOpen? = null
    var elseCloseCurlyToken: Token.CurlyBracketClose? = null

    skipTokens<Token.EOL>()

    if (!isToken<Token.CurlyBracketOpen>()) {
        elseBody = listOf(
            parseStatement(parseUntilCondition)
                ?: throw UnexpectedTokenException("statement", getToken()),
        )
        if (isToken<Token.EOL>()) advance()
    } else {
        elseOpenCurlyToken = expectToken<Token.CurlyBracketOpen>()
        elseBody = parseListOfStatements { it !is Token.CurlyBracketClose }
        elseCloseCurlyToken = expectToken<Token.CurlyBracketClose>()
        skipTokens<Token.EOL>()
    }

    return ElseDeclaration(
        elseToken = elseToken,
        elseOpenCurlyToken = elseOpenCurlyToken,
        elseCloseCurlyToken = elseCloseCurlyToken,
        elseBody = elseBody,
    )
}
