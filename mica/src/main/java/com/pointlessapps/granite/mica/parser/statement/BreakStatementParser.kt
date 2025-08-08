package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.BreakStatement
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseBreakStatement(): BreakStatement {
    val breakToken = expectToken<Token.Keyword> { it.value == Keyword.BREAK.value }
    if (getToken().let { it is Token.EOL || it is Token.EOF }) {
        expectEOForEOL()
        return BreakStatement(breakToken)
    }

    return BreakStatement(breakToken)
}
