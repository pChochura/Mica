package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that contains a function call. Optionally a type argument can be passed to a function
 * by using the `@` symbol. It can be used in the function body as a type hint.
 * Trailing commas and arguments on different lines are allowed.
 *
 * Examples:
 *  - `method()`
 *  - `method(1)`
 *  - `method(1, "2")`
 *  - `method@int(1, "2")`
 */
internal class FunctionCallExpression(
    val nameToken: Token.Symbol,
    val openBracketToken: Token.BracketOpen,
    val closeBracketToken: Token.BracketClose,
    val atToken: Token.At?,
    val typeArgument: TypeExpression?,
    val arguments: List<Expression>,
    val isMemberFunctionCall: Boolean,
) : Expression(nameToken)
