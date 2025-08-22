package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that represents a postfix increment or decrement assignment.
 *
 * Examples:
 *  - `a++`
 *  - `b--`
 */
internal class PostfixAssignmentExpression(
    val symbolToken: Token.Symbol,
    val operatorToken: Token,
) : Expression(symbolToken)
