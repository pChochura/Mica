package com.pointlessapps.granite.mica.parser

import com.pointlessapps.granite.mica.model.Token

internal fun Parser.isVariableDeclarationStatementStarting(): Boolean {
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

    advance()
    while (isToken<Token.SquareBracketOpen>()) advance()
    if (!isToken<Token.Symbol>()) {
        restoreTo(currentIndex)
        return false
    }
    advance()
    while (isToken<Token.SquareBracketClose>()) advance()

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
        restoreTo(currentIndex)
        return false
    }

    advance()
    if (!isToken<Token.Colon>()) {
        restoreTo(savedIndex)
        return false
    }

    advance()
    while (isToken<Token.SquareBracketOpen>()) advance()
    if (!isToken<Token.Symbol>()) {
        restoreTo(currentIndex)
        return false
    }
    advance()
    while (isToken<Token.SquareBracketClose>()) advance()

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
        restoreTo(currentIndex)
        return false
    }

    advance()
    if (!isToken<Token.Equals>() && !isToken<Token.PlusEquals>() && !isToken<Token.MinusEquals>()) {
        restoreTo(savedIndex)
        return false
    }

    restoreTo(savedIndex)
    return true
}

internal fun Parser.isArrayAssignmentStatementStarting(): Boolean {
    val savedIndex = currentIndex
    if (!isToken<Token.Symbol>()) {
        restoreTo(currentIndex)
        return false
    }

    advance()
    while (isToken<Token.SquareBracketOpen>()) {
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

internal fun Parser.isFunctionCallStatementStarting(): Boolean {
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

internal fun Parser.isPostfixUnaryExpressionStarting(): Boolean {
    val savedIndex = currentIndex
    if (!isToken<Token.Symbol>()) {
        restoreTo(currentIndex)
        return false
    }

    advance()
    while (isToken<Token.SquareBracketOpen>()) {
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
