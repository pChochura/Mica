package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that assigns a value to a variable.
 *
 * Examples:
 *  - `a = 1`
 *  - `a = b`
 *  - `a = a + 1 - callToADifferentFunction()`
 *  - `a += 1`
 *  - `a -= 10`
 */
internal class AssignmentStatement(
    val lhsToken: Token.Symbol,
    val equalSignToken: Token,
    val rhs: Expression,
) : Statement(lhsToken)
