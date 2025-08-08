package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseIfConditionStatement(
    parseUntilCondition: (Token) -> Boolean,
): IfConditionStatement {
    val ifConditionDeclaration = parseIfConditionDeclaration(parseUntilCondition)
    if (getToken().let { it !is Token.Keyword || it.value != Keyword.ELSE.value }) {
        expectEOForEOL()

        return IfConditionStatement(
            ifConditionDeclaration = ifConditionDeclaration,
            elseIfConditionDeclarations = null,
            elseDeclaration = null,
        )
    }

    val elseIfConditionDeclarations = parseElseIfConditionDeclarations(parseUntilCondition)
    val elseDeclaration = parseElseDeclaration(parseUntilCondition)

    expectEOForEOL()

    return IfConditionStatement(
        ifConditionDeclaration = ifConditionDeclaration,
        elseIfConditionDeclarations = elseIfConditionDeclarations.takeIf { it.isNotEmpty() },
        elseDeclaration = elseDeclaration,
    )
}
