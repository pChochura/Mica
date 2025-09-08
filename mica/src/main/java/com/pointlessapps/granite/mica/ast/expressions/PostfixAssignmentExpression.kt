package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.ast.AccessorExpression
import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that represents a postfix increment or decrement assignment.
 *
 * Examples:
 *  - `a++`
 *  - `b--`
 *  - `a[1]++`
 *  - `b[0][1]--`
 *  - `c.property--`
 *  - `c[1].property[0]++`
 */
internal class PostfixAssignmentExpression(
    symbolToken: Token.Symbol,
    accessorExpressions: List<AccessorExpression>,
    operatorToken: Token,
) : AffixAssignmentExpression(operatorToken, symbolToken, accessorExpressions)
