package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * An expression that is used to coerce a value to have a specified type.
 * The underlying value is not modified. Those coercions aren't safe.
 *
 * Examples:
 *  - `method() as int`
 *  - `array[0] as [string]`
 *  - `"some long string" as [char]`
 */
internal class TypeCoercionExpression(
    val lhs: Expression,
    val asToken: Token.Keyword,
    val typeExpression: TypeExpression,
) : Expression(lhs.startingToken)
