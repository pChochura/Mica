package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.ast.ArrayIndex
import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that represents a prefix increment or decrement assignment.
 *
 * Examples:
 *  - `++a`
 *  - `--b`
 *  - `++a[1]`
 *  - `--b[0][1]`
 */
internal class PrefixAssignmentExpression(
    operatorToken: Token,
    symbolToken: Token.Symbol,
    indexExpressions: List<ArrayIndex>,
) : AffixAssignmentExpression(operatorToken, symbolToken, indexExpressions)
