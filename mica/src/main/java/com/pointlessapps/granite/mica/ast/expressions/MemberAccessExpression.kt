package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that contains an access to a member call.
 *
 * Examples:
 *  - `a.b`
 *  - `b.property`
 */
internal class MemberAccessExpression(
    val lhs: Expression,
    val dotToken: Token.Dot,
    val propertySymbolToken: Token.Symbol,
) : Expression(lhs.startingToken)
