package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that assigns a value to a variable.
 *
 * Examples:
 *  - `a = 1`
 *  - `a.first = b`
 *  - `a = a + 1 - callToADifferentFunction()`
 *  - `a.property.a += 1`
 *  - `a[0].property[1] -= 10`
 *  - `a[0].a[0][1].b = 1`
 *  - `a[3] += 10`
 *  - `a[1] -= 2`
 *  - `a[calculateIndex()] = b`
 *  - `a[0] = a[0] + 1 - callToADifferentFunction()`
 *  - `a[0][1] = 1`
 */
internal class AssignmentStatement(
    val symbolToken: Token.Symbol,
    val accessorExpressions: List<AccessorExpression>,
    val equalSignToken: Token,
    val rhs: Expression,
) : Statement(symbolToken)
