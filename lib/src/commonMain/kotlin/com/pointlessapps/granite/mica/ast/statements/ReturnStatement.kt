package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.model.ScopeType.Loop
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that represents a return call. The return value is optional.
 * It is only allowed inside of a [ScopeType.Function].
 *
 * Examples:
 *  - `return`
 *  - `return 1`
 */
internal class ReturnStatement(
    val returnToken: Token.Keyword,
    val returnExpression: Expression?,
) : Statement(returnToken)
