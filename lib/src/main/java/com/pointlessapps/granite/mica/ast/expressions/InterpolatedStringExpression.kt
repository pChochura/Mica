package com.pointlessapps.granite.mica.ast.expressions

/**
 * An expression that represents an interpolated string, meaning a string that is capable
 * of injecting expressions into it.
 *
 *
 * Examples:
 *  - `"$(a)"`
 *  - `"Result of a + b: $(a + b)"`
 *  - `"Multiple injections $(a[0]]) and $(a[1]) and something at the end"`
 */
internal open class InterpolatedStringExpression(
    val expressions: List<Expression>,
) : Expression(expressions.first().startingToken)
