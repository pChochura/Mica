package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that contains a function call.
 *
 * Examples:
 *  - `method()`
 *  - `method(1)`
 *  - `method(1, "2")`
 */
internal class FunctionCallExpression(
    val nameToken: Token.Symbol,
    val openBracketToken: Token.BracketOpen,
    val closeBracketToken: Token.BracketClose,
    val arguments: List<Expression>,
    val isMemberFunctionCall: Boolean,
) : Expression(nameToken)
