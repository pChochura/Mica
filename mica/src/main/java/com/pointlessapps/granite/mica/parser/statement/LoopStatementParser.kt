package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.LoopInStatement
import com.pointlessapps.granite.mica.ast.statements.LoopStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression
import com.pointlessapps.granite.mica.parser.isLoopInExpressionStarting

internal fun Parser.parseLoopStatement(
    parseUntilCondition: (Token) -> Boolean,
): LoopStatement {
    val loopToken = expectToken<Token.Keyword>("loop statement") {
        it.value == Keyword.LOOP.value
    }

    val ifToken = if (getToken().let { it is Token.Keyword && it.value == Keyword.IF.value }) {
        expectToken<Token.Keyword>("loop if statement") { it.value == Keyword.IF.value }
    } else {
        null
    }

    if (ifToken == null && isLoopInExpressionStarting()) {
        val symbolToken = expectToken<Token.Symbol>("loop in statement") { it !is Token.Keyword }
        val inToken = expectToken<Token.Keyword>("loop in statement") {
            it.value == Keyword.IN.value
        }
        val arrayExpression = parseExpression {
            parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
        } ?: throw UnexpectedTokenException("expression", getToken(), "loop in statement")

        val loopBody = parseBlockBody(parseUntilCondition)

        return LoopInStatement(
            loopToken = loopToken,
            symbolToken = symbolToken,
            inToken = inToken,
            arrayExpression = arrayExpression,
            loopBody = loopBody,
        )
    }

    val conditionExpression = if (ifToken != null) {
        parseExpression {
            parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
        } ?: throw UnexpectedTokenException("expression", getToken(), "loop if statement")
    } else {
        null
    }

    val loopBody = parseBlockBody(parseUntilCondition)

    if (getToken().let { it !is Token.Keyword || it.value != Keyword.ELSE.value }) {
        return LoopIfStatement(
            loopToken = loopToken,
            ifToken = ifToken,
            ifConditionExpression = conditionExpression,
            loopBody = loopBody,
            elseDeclaration = null,
        )
    }

    val elseDeclaration = parseElseDeclaration(parseUntilCondition)

    expectEOForEOL("loop statement")

    return LoopIfStatement(
        loopToken = loopToken,
        ifToken = ifToken,
        ifConditionExpression = conditionExpression,
        loopBody = loopBody,
        elseDeclaration = elseDeclaration,
    )
}
