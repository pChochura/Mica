package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expressions that contain function calls with a return value.
 * Examples:
 *  - `method()`
 *  - `method(1)`
 *  - `method(1, "2")`
 *
 *  Where:
 *  ```
 *  method(): number {
 *      return 0
 *  }
 *  method(a: number): string {
 *      return "0"
 *  }
 *  method(a: number, b: string): boolean {
 *      return false
 *  }
 *  ```
 */
internal class FunctionCallExpression(
    val nameToken: Token.Symbol,
    val openBracketToken: Token.BracketOpen,
    val closeBracketToken: Token.BracketClose,
    val arguments: List<Expression>,
) : Expression(nameToken) {

    fun getSignature(): String {
        // TODO get the signature
        return "${nameToken.value}()"
    }
}
