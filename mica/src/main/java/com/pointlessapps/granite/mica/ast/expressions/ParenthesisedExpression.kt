package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

internal class ParenthesisedExpression(
    val openBracketToken: Token.BracketOpen,
    val closeBracketToken: Token.BracketClose,
    val expression: Expression,
) : Expression(openBracketToken)
