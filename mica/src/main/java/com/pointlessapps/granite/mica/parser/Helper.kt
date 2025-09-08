package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token

internal fun Parser.isVariableDeclarationStatementStarting(): Boolean {
    val savedIndex = currentIndex
    if (!isToken<Token.Symbol>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    if (!isToken<Token.Colon>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    while (isToken<Token.SquareBracketOpen>() || isToken<Token.CurlyBracketOpen>()) advance()
    if (!isToken<Token.Symbol>()) {
        restoreTo(savedIndex)
        return false
    }
    advance()
    while (isToken<Token.SquareBracketClose>() || isToken<Token.CurlyBracketClose>()) advance()

    if (!isToken<Token.Equals>()) {
        restoreTo(savedIndex)
        return false
    }

    restoreTo(savedIndex)
    return true
}

internal fun Parser.isPropertyDeclarationStatementStarting(): Boolean {
    val savedIndex = currentIndex
    if (!isToken<Token.Symbol>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    if (!isToken<Token.Colon>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    while (isToken<Token.SquareBracketOpen>() || isToken<Token.CurlyBracketOpen>()) advance()
    if (!isToken<Token.Symbol>()) {
        restoreTo(savedIndex)
        return false
    }
    advance()
    while (isToken<Token.SquareBracketClose>() || isToken<Token.CurlyBracketClose>()) advance()

    if (!isToken<Token.EOL>()) {
        restoreTo(savedIndex)
        return false
    }

    restoreTo(savedIndex)
    return true
}

internal fun Parser.isAssignmentStatementStarting(): Boolean {
    val savedIndex = currentIndex
    if (!isToken<Token.Symbol>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    while (isToken<Token.SquareBracketOpen>() || isToken<Token.Dot>()) {
        if (isToken<Token.Dot>()) {
            advance()
            if (!isToken<Token.Symbol>()) {
                restoreTo(savedIndex)
                return false
            }

            advance()
            continue
        }

        while (!isToken<Token.SquareBracketClose>()) {
            if (isToken<Token.EOL>()) {
                restoreTo(savedIndex)
                return false
            }
            advance()
        }
        advance()
    }

    if (!isToken<Token.Equals>() && !isToken<Token.PlusEquals>() && !isToken<Token.MinusEquals>()) {
        restoreTo(savedIndex)
        return false
    }

    restoreTo(savedIndex)
    return true
}

internal fun Parser.isFunctionDeclarationStatementStarting(): Boolean {
    val savedIndex = currentIndex
    if (!isToken<Token.Symbol>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    if (!isToken<Token.BracketOpen>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    var bracketsCount = 0
    while (!isToken<Token.BracketClose>() || bracketsCount > 0) {
        if (isToken<Token.BracketOpen>()) bracketsCount++
        if (isToken<Token.BracketClose>()) bracketsCount--
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

internal fun Parser.isFunctionCallStatementStarting(): Boolean {
    val savedIndex = currentIndex
    if (!isToken<Token.Symbol>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    if (!isToken<Token.BracketOpen>()) {
        restoreTo(savedIndex)
        return false
    }

    restoreTo(savedIndex)
    return true
}

internal fun Parser.isPostfixAssignmentExpressionStarting(): Boolean {
    val savedIndex = currentIndex
    if (!isToken<Token.Symbol>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    while (isToken<Token.SquareBracketOpen>() || isToken<Token.Dot>()) {
        if (isToken<Token.Dot>()) {
            advance()
            if (!isToken<Token.Symbol>()) {
                restoreTo(savedIndex)
                return false
            }

            advance()
            continue
        }

        while (!isToken<Token.SquareBracketClose>()) {
            if (isToken<Token.EOL>()) {
                restoreTo(savedIndex)
                return false
            }
            advance()
        }
        advance()
    }

    if (!isToken<Token.Increment>() && !isToken<Token.Decrement>()) {
        restoreTo(savedIndex)
        return false
    }

    restoreTo(savedIndex)
    return true
}

internal fun Parser.isLoopInExpressionStarting(): Boolean {
    val savedIndex = currentIndex
    if (!isToken<Token.Symbol>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    if (isToken<Token.Comma>()) {
        advance()
        if (!isToken<Token.Symbol>()) {
            restoreTo(savedIndex)
            return false
        }
        advance()
    }

    if (getToken().let { it !is Token.Keyword || it.value != Keyword.IN.value }) {
        restoreTo(savedIndex)
        return false
    }

    restoreTo(savedIndex)
    return true
}
