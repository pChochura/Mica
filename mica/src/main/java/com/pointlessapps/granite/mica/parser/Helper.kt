package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.model.Token

internal object Helper {
    fun Parser.isVariableDeclarationStatementStarting(): Boolean {
        val savedIndex = currentIndex
        if (!isToken<Token.Symbol>()) {
            restoreTo(currentIndex)
            return false
        }

        advance()
        if (!isToken<Token.Colon>()) {
            restoreTo(savedIndex)
            return false
        }

        restoreTo(savedIndex)
        return true
    }

    fun Parser.isAssignmentStatementStarting(): Boolean {
        val savedIndex = currentIndex
        if (!isToken<Token.Symbol>()) {
            restoreTo(currentIndex)
            return false
        }

        advance()
        if (!isToken<Token.Equals>()) {
            restoreTo(savedIndex)
            return false
        }

        restoreTo(savedIndex)
        return true
    }

    fun Parser.isFunctionDeclarationStatementStarting(): Boolean {
        val savedIndex = currentIndex
        if (!isToken<Token.Symbol>()) {
            restoreTo(currentIndex)
            return false
        }

        advance()
        if (!isToken<Token.BracketOpen>()) {
            restoreTo(savedIndex)
            return false
        }

        advance()
        while (!isToken<Token.BracketClose>()) {
            if (isToken<Token.EOL>()) {
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

    fun Parser.isFunctionCallStatementStarting(): Boolean {
        val savedIndex = currentIndex
        if (!isToken<Token.Symbol>()) {
            restoreTo(currentIndex)
            return false
        }

        advance()
        if (!isToken<Token.BracketOpen>()) {
            restoreTo(savedIndex)
            return false
        }

        advance()
        while (!isToken<Token.BracketClose>()) {
            if (isToken<Token.EOL>()) {
                restoreTo(savedIndex)
                return false
            }

            advance()
        }

        restoreTo(savedIndex)
        return true
    }
}
