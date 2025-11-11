package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that represents a prefix increment or decrement assignment.
 *
 * Examples:
 *  - `++a`
 *  - `--b`
 *  - `++a[1]`
 *  - `--b[0][1]`
 *  - `--c.property`
 *  - `++c[1].property[0]`
 */
internal class PrefixAssignmentExpression(
    operatorToken: Token,
    symbolToken: Token.Symbol,
    accessorExpressions: List<AccessorExpression>,
) : AffixAssignmentExpression(operatorToken, symbolToken, accessorExpressions)
