package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that represents a break call. It is only allowed inside of a [ScopeType.Loop].
 * It stops the execution of the loop.
 *
 * Examples:
 *  ```
 *  variable = 0
 *  loop {
 *    if variable++ > 10 {
 *      break
 *    }
 *  }
 *  ```
 */
internal class BreakStatement(
    val breakToken: Token.Keyword,
) : Statement(breakToken)
