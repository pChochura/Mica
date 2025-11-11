package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * An expression that represents an array literal which is a list of expressions
 * enclosed in square brackets.
 * Trailing commas and values on different lines are allowed.
 *
 * Examples:
 *  - `[]` - empty array literal
 *  - `[1, 2, 3]`
 *  - `[1, "2", true]`
 *  - `[method(), method(1)]`
 *  - `[[1, 2], ["text", 'char']]`
 */
internal class ArrayLiteralExpression(
    val openBracketToken: Token.SquareBracketOpen,
    val closeBracketToken: Token.SquareBracketClose,
    val elements: List<Expression>,
) : Expression(openBracketToken)
