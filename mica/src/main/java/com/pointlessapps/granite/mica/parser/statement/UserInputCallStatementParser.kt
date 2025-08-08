package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser

internal fun Parser.parseUserInputCallStatement(): UserInputCallStatement {
    val userInputStartingToken = expectToken<Token.Operator> {
        it.type == Token.Operator.Type.LessThan
    }
    val stringLiteralToken = expectToken<Token.Symbol>()

    return UserInputCallStatement(userInputStartingToken, stringLiteralToken)
}
