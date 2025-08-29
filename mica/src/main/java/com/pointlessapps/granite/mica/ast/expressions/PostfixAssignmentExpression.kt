package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.ast.ArrayIndex
import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that represents a postfix increment or decrement assignment.
 *
 * Examples:
 *  - `a++`
 *  - `b--`
 *  - `a[1]++`
 *  - `b[0][1]--`
 */
internal class PostfixAssignmentExpression(
    symbolToken: Token.Symbol,
    indexExpressions: List<ArrayIndex>,
    operatorToken: Token,
) : AffixAssignmentExpression(operatorToken, symbolToken, indexExpressions)
