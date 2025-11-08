package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.ast.AccessorExpression

/**
 * An expression that is used to access a specific element in a type.
 * The [accessorExpressions] will be evaluated to an integer which will be used
 * as an index of an array or a string which will be used as a property name of the custom object.
 *
 * Examples:
 *  - `array[0].property[0].a`
 *  - `array[10 * methodCall()].property`
 *  - `"some long string"[0]`
 *  - `[1, 2, 3][1]`
 *  - `(1..5)[2]`
 */
internal class MemberAccessExpression(
    val symbolExpression: Expression,
    val accessorExpressions: List<AccessorExpression>,
) : Expression(symbolExpression.startingToken)
