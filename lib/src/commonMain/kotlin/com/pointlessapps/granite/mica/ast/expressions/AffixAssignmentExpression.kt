package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Helper class that holds information about the prefix or postfix operation
 * on a symbol or an array-like variable.
 *
 * Examples:
 *  - `++a`
 *  - `b--`
 *  - `++a[1]`
 *  - `b[0][1]--`
 *  - `--c.property`
 *  - `c[1].property[0]++`
 */
internal open class AffixAssignmentExpression(
    val operatorToken: Token,
    val symbolToken: Token.Symbol,
    val accessorExpressions: List<AccessorExpression>,
) : Expression(operatorToken)
