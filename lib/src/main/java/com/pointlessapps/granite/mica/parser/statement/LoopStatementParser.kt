package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.LoopInStatement
import com.pointlessapps.granite.mica.ast.statements.LoopStatement
import com.pointlessapps.granite.mica.errors.UnexpectedTokenException
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseBlockBody
import com.pointlessapps.granite.mica.parser.expression.parseElseDeclaration
import com.pointlessapps.granite.mica.parser.expression.parseExpression

internal fun Parser.parseLoopStatement(
    parseUntilCondition: (Token) -> Boolean,
): LoopStatement {
    val loopToken = expectToken<Token.Keyword>("loop statement") { it.value == Keyword.LOOP.value }

    val savedIndex = currentIndex
    skipTokens<Token.EOL>()
    if (isToken<Token.CurlyBracketOpen>()) {
        return LoopIfStatement(
            loopToken = loopToken,
            ifToken = null,
            ifConditionExpression = null,
            loopBody = parseBlockBody(parseUntilCondition),
            elseDeclaration = null,
        )
    }
    restoreTo(savedIndex)

    if (getToken().let { it is Token.Keyword && it.value == Keyword.IF.value }) {
        return parseLoopIfStatement(loopToken, parseUntilCondition)
    }

    return parseLoopInStatement(loopToken, parseUntilCondition)
}

private fun Parser.parseLoopInStatement(
    loopToken: Token.Keyword,
    parseUntilCondition: (Token) -> Boolean,
): LoopStatement {
    val symbolToken = expectToken<Token.Symbol>("loop in statement") { it !is Token.Keyword }
    var commaToken: Token.Comma? = null
    var indexToken: Token.Symbol? = null
    if (isToken<Token.Comma>()) {
        commaToken = expectToken<Token.Comma>("loop in statement")
        indexToken = expectToken<Token.Symbol>("loop in statement") { it !is Token.Keyword }
    }

    val inToken = expectToken<Token.Keyword>("loop in statement") {
        it.value == Keyword.IN.value
    }
    val arrayExpression = parseExpression {
        parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
    } ?: throw UnexpectedTokenException("expression", getToken(), "loop in statement")

    val loopBody = parseBlockBody(parseUntilCondition)

    expectEOForEOL("loop in statement")
    return LoopInStatement(
        loopToken = loopToken,
        symbolToken = symbolToken,
        commaToken = commaToken,
        indexToken = indexToken,
        inToken = inToken,
        arrayExpression = arrayExpression,
        loopBody = loopBody,
    )
}

private fun Parser.parseLoopIfStatement(
    loopToken: Token.Keyword,
    parseUntilCondition: (Token) -> Boolean,
): LoopIfStatement {
    val ifToken = expectToken<Token.Keyword>("loop if statement") { it.value == Keyword.IF.value }
    val conditionExpression = parseExpression {
        parseUntilCondition(it) || it is Token.CurlyBracketOpen || it is Token.EOL
    } ?: throw UnexpectedTokenException("expression", getToken(), "loop if statement")

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
