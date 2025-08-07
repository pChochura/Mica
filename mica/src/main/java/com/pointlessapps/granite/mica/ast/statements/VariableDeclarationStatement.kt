package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.linter.mapper.toType

/**
 * Statement that declares a variable.
 *
 * Examples:
 *  - `a: number = 123`
 *  - `b: string = "This is a string"`
 */
internal class VariableDeclarationStatement(
    val lhsToken: Token.Symbol,
    val colonToken: Token.Colon,
    val typeToken: Token.Symbol,
    val equalSignToken: Token.Equals,
    val rhs: Expression,
) : Statement(lhsToken) {

    // TODO add support for multiple declarations at once

    val type = typeToken.toType()
}
