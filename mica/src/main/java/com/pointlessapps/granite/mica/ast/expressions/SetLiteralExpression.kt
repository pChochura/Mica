package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * An expression that represents a set literal which is a group of expressions
 * enclosed in curly brackets. The type ensures that an element won't be repeated in the set.
 * Trailing commas and values on different lines are allowed.
 *
 * Examples:
 *  - `{}` - empty set literal
 *  - `{1, 2, 3}`
 *  - `{1, "2", true}`
 *  - `{method(), method(1)}`
 *  - `{{1, 2}, {"text", 'char'}}`
 */
internal class SetLiteralExpression(
    val openBracketToken: Token.CurlyBracketOpen,
    val closeBracketToken: Token.CurlyBracketClose,
    val elements: List<Expression>,
) : Expression(openBracketToken)
