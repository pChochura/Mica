package com.pointlessapps.granite.mica.parser.expression

import com.pointlessapps.granite.mica.ast.expressions.BlockBody
import com.pointlessapps.granite.mica.ast.expressions.ElseDeclaration
import com.pointlessapps.granite.mica.ast.expressions.ElseIfConditionDeclaration
import com.pointlessapps.granite.mica.ast.expressions.IfConditionDeclaration
import com.pointlessapps.granite.mica.ast.expressions.IfConditionExpression
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.statement.parseListOfStatements
import com.pointlessapps.granite.mica.parser.statement.parseStatement

internal fun Parser.parseIfConditionExpression(
    parseUntilCondition: (Token) -> Boolean,
): IfConditionExpression {
    val ifConditionDeclaration = parseIfConditionDeclaration(parseUntilCondition)
    val elseIfConditionDeclarations = parseElseIfConditionDeclarations(parseUntilCondition)
    val elseDeclaration = parseElseDeclaration(parseUntilCondition)

    return IfConditionExpression(
        ifConditionDeclaration = ifConditionDeclaration,
        elseIfConditionDeclarations = elseIfConditionDeclarations.takeIf { it.isNotEmpty() },
        elseDeclaration = elseDeclaration,
    )
}

internal fun Parser.parseIfConditionDeclaration(
    parseUntilCondition: (Token) -> Boolean,
): IfConditionDeclaration {
    val ifToken = expectToken<Token.Keyword>("if expression") { it.value == Keyword.IF.value }
    val expression = parseExpression {
        parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
    } ?: throw UnexpectedTokenException("expression", getToken(), "if expression")

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
        val elseToken = expectToken<Token.Keyword>("else if expression") {
            it.value == Keyword.ELSE.value
        }
        if (getToken().let { it !is Token.Keyword || it.value != Keyword.IF.value }) {
            restoreTo(savedIndex)

            break
        }

        val elseIfToken = expectToken<Token.Keyword>("else if expression") {
            it.value == Keyword.IF.value
        }
        val elseIfExpression = parseExpression {
            parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
        } ?: throw UnexpectedTokenException("expression", getToken(), "else if expression")

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

    val elseToken = expectToken<Token.Keyword>("else expression") { it.value == Keyword.ELSE.value }

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
            parseStatement {
                parseUntilCondition(it) || it is Token.EOL || it is Token.EOF
            } ?: throw UnexpectedTokenException("expression", getToken(), "if expression"),
        )
    } else {
        openCurlyToken = expectToken<Token.CurlyBracketOpen>("if expression")
        body = parseListOfStatements { it is Token.CurlyBracketClose }
        closeCurlyToken = expectToken<Token.CurlyBracketClose>("if expression")
    }

    return BlockBody(openCurlyToken, closeCurlyToken, body)
}
