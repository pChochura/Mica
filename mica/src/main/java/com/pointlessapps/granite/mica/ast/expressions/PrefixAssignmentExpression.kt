package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that represents a prefix increment or decrement assignment.
 *
 * Examples:
 *  - `++a`
 *  - `--b`
 */
internal class PrefixAssignmentExpression(
    val operatorToken: Token,
    val symbolToken: Token.Symbol,
) : Expression(operatorToken)
