package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.ast.ArrayIndex
import com.pointlessapps.granite.mica.model.Token

/**
 * Helper class that holds information about the prefix or postfix operation on a symbol or array-like variable.
 *
 * Examples:
 *  - `++a`
 *  - `--b`
 *  - `++a[1]`
 *  - `--b[0][1]`
 */
internal open class AffixAssignmentExpression(
    val operatorToken: Token,
    val symbolToken: Token.Symbol,
    val indexExpressions: List<ArrayIndex>,
) : Expression(operatorToken)
