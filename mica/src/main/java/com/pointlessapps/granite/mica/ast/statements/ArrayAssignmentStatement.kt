package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.ArrayIndex
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that assigns a value to an array-like variable at the given index.
 *
 * Examples:
 *  - `a[0] = 1`
 *  - `a[3] += 10`
 *  - `a[1] -= 2`
 *  - `a[calculateIndex()] = b`
 *  - `a[0] = a[0] + 1 - callToADifferentFunction()`
 *  - `a[0][1] = 1`
 */
internal class ArrayAssignmentStatement(
    val arraySymbolToken: Token.Symbol,
    val indexExpressions: List<ArrayIndex>,
    val equalSignToken: Token,
    val rhs: Expression,
) : Statement(arraySymbolToken)
