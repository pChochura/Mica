package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that represents a user input call. The execution of that statement
 * saves the user input in a variable provided by the [contentToken].
 *
 * Examples:
 *  - `< input`
 */
internal class UserInputCallStatement(
    val inputToken: Token.Operator,
    val contentToken: Token.Symbol,
) : Statement(inputToken)

// TODO add a declaration of the input variable if it doesnt exist
