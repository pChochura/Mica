package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * Expression that contains a constructor call for a type.
 * Trailing commas and arguments on different lines are allowed.
 *
 * Examples:
 *  - `type { property1 = value1, property2 = value2 }`
 *  ```
 *  type pair {
 *    first: int
 *    second: int
 *  }
 *  a = pair {
 *    first = 4,
 *    second = 12,
 *  }
 *  b = pair { first = 1, second = 2 }
 *  ```
 */
internal class ConstructorCallExpression(
    val nameToken: Token.Symbol,
    val openCurlyBracketToken: Token.CurlyBracketOpen,
    val closeCurlyBracketToken: Token.CurlyBracketClose,
    val propertyValuePairs: List<PropertyValuePair>,
) : Expression(nameToken)

internal class PropertyValuePair(
    val propertyName: Token.Symbol,
    val equalsToken: Token.Equals,
    val valueExpression: Expression,
)
