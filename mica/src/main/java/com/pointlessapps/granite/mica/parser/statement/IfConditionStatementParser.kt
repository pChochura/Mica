package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.BlockBody
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
    val ifToken = expectToken<Token.Keyword>("if statement") { it.value == Keyword.IF.value }
    val expression = parseExpression {
        parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
    } ?: throw UnexpectedTokenException("expression", getToken(), "if statement")

    return IfConditionDeclaration(
        ifToken = ifToken,
        ifConditionExpression = expression,
        ifBody = parseBlockBody(parseUntilCondition),
    )
}

internal fun Parser.parseElseIfConditionDeclarations(
    parseUntilCondition: (Token) -> Boolean,
): List<ElseIfConditionDeclaration> {
    val elseIfConditionDeclarations = mutableListOf<ElseIfConditionDeclaration>()
    while (getToken().let { it is Token.Keyword && it.value == Keyword.ELSE.value }) {
        val savedIndex = currentIndex
        val elseToken = expectToken<Token.Keyword>("else if statement") {
            it.value == Keyword.ELSE.value
        }
        if (getToken().let { it !is Token.Keyword || it.value != Keyword.IF.value }) {
            restoreTo(savedIndex)

            break
        }

        val elseIfToken = expectToken<Token.Keyword>("else if statement") {
            it.value == Keyword.IF.value
        }
        val elseIfExpression = parseExpression {
            parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
        } ?: throw UnexpectedTokenException("expression", getToken(), "else if statement")

        elseIfConditionDeclarations.add(
            ElseIfConditionDeclaration(
                elseIfToken = elseToken to elseIfToken,
                elseIfConditionExpression = elseIfExpression,
                elseIfBody = parseBlockBody(parseUntilCondition),
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

    val elseToken = expectToken<Token.Keyword>("else statement") { it.value == Keyword.ELSE.value }

    return ElseDeclaration(
        elseToken = elseToken,
        elseBody = parseBlockBody(parseUntilCondition),
    )
}

internal fun Parser.parseBlockBody(parseUntilCondition: (Token) -> Boolean): BlockBody {
    var body: List<Statement>
    var openCurlyToken: Token.CurlyBracketOpen? = null
    var closeCurlyToken: Token.CurlyBracketClose? = null

    skipTokens<Token.EOL>()

    if (!isToken<Token.CurlyBracketOpen>()) {
        body = listOf(
            parseStatement(parseUntilCondition)
                ?: throw UnexpectedTokenException("statement", getToken(), "if statement"),
        )
        if (isToken<Token.EOL>()) advance()
    } else {
        openCurlyToken = expectToken<Token.CurlyBracketOpen>("if statement")
        body = parseListOfStatements { it !is Token.CurlyBracketClose }
        closeCurlyToken = expectToken<Token.CurlyBracketClose>("if statement")
        skipTokens<Token.EOL>()
    }

    return BlockBody(openCurlyToken, closeCurlyToken, body)
}
