package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseExpression

internal fun Parser.parseLoopIfStatement(
    parseUntilCondition: (Token) -> Boolean,
): LoopIfStatement {
    val loopToken = expectToken<Token.Keyword>("loop if statement") {
        it.value == Keyword.LOOP.value
    }

    val ifToken = if (getToken().let { it is Token.Keyword && it.value == Keyword.IF.value }) {
        expectToken<Token.Keyword>("loop if statement") { it.value == Keyword.IF.value }
    } else {
        null
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

    expectEOForEOL("loop if statement")

    return LoopIfStatement(
        loopToken = loopToken,
        ifToken = ifToken,
        ifConditionExpression = conditionExpression,
        loopBody = loopBody,
        elseDeclaration = elseDeclaration,
    )
}
