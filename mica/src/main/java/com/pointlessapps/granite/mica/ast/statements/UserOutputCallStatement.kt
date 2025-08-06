package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that represents a user output call. The execution of that statement
 * prints the value of the [contentExpression].
 *
 * Examples:
 *  - `> "This will be printed"`
 *  - `> "Strings" + " " + "can be concatenated" + a`
 *  - `> variable`
 */
internal class UserOutputCallStatement(
    val outputToken: Token.Operator,
    val contentExpression: Expression,
) : Statement(outputToken)
