package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.model.Token

internal fun Parser.isFunctionDeclarationStatementStarting(): Boolean {
    val savedIndex = currentIndex
    if (isToken<Token.At>()) {
        advance()
        while (!isToken<Token.BracketOpen>()) {
            if (isToken<Token.EOF>() || isToken<Token.EOL>()) {
                restoreTo(savedIndex)
                return false
            }
            advance()
        }
    }

    if (!isToken<Token.BracketOpen>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    var bracketsCount = 0
    while (!isToken<Token.BracketClose>() || bracketsCount > 0) {
        if (isToken<Token.BracketOpen>()) bracketsCount++
        if (isToken<Token.BracketClose>()) bracketsCount--
        if (isToken<Token.EOF>()) {
            restoreTo(savedIndex)
            return false
        }
        advance()
    }

    advance()
    if (!isToken<Token.Colon>()) {
        skipTokens<Token.EOL>()
        if (!isToken<Token.CurlyBracketOpen>()) {
            restoreTo(savedIndex)
            return false
        }
    }

    restoreTo(savedIndex)
    return true
}
