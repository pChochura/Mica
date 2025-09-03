package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that represents a user input call. The execution of that statement
 * saves the user input in a variable provided by the [contentToken].
 * If the variable doesnt exist, it will be created in that scope with a `string` type.
 *
 * Examples:
 *  - `< input`
 */
internal class UserInputCallStatement(
    val inputToken: Token.Operator,
    val contentToken: Token.Symbol,
) : Statement(inputToken)
