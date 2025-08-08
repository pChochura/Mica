package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseLoopIfStatement(
    parseUntilCondition: (Token) -> Boolean,
): LoopIfStatement {
    val loopToken = expectToken<Token.Keyword> { it.value == Keyword.LOOP.value }
    val ifConditionDeclaration = parseIfConditionDeclaration(parseUntilCondition)
    if (getToken().let { it !is Token.Keyword || it.value != Keyword.ELSE.value }) {
        expectEOForEOL()

        return LoopIfStatement(
            loopToken = loopToken,
            ifConditionDeclaration = ifConditionDeclaration,
            elseDeclaration = null,
        )
    }

    val elseDeclaration = parseElseDeclaration(parseUntilCondition)

    expectEOForEOL()

    return LoopIfStatement(
        loopToken = loopToken,
        ifConditionDeclaration = ifConditionDeclaration,
        elseDeclaration = elseDeclaration,
    )
}
