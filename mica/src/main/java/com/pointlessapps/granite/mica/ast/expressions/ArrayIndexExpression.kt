package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * An expression that is used to access a specific element in an array.
 * The [indexExpression] will be evaluated to an integer which will be used as an index
 * or an array of integers that will be used as indices.
 * [arrayExpression] will be evaluated to an array in which the element will be accessed.
 *
 * Examples:
 *  - `array[0]`
 *  - `array[10 * methodCall()]`
 *  - `"some long string"[0]`
 *  - `[1, 2, 3][1]`
 *  - `(1..5)[2]`
 */
internal class ArrayIndexExpression(
    val arrayExpression: Expression,
    val openBracketToken: Token.SquareBracketOpen,
    val closeBracketToken: Token.SquareBracketClose,
    val indexExpression: Expression,
) : Expression(arrayExpression.startingToken)
