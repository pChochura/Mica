package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

/**
 * An expression that represents a map literal which is a group of pairs of expressions
 * enclosed in curly brackets. The type ensures that an element won't be repeated for a given key.
 * Trailing commas and values on different lines are allowed.
 *
 * Examples:
 *  - `{:}` - empty map literal
 *  - `{1: "a", 2: "b", 3: "c"}`
 *  - `{"a": 1, "b": "2", "c": true}`
 *  - `{[1]: method(), [2]: method(1)}`
 *  - `{1.0: {1, 2}, 2.0: {"text", 'char'}}`
 */
internal class MapLiteralExpression(
    val openBracketToken: Token.CurlyBracketOpen,
    val closeBracketToken: Token.CurlyBracketClose,
    val keyValuePairs: List<KeyValuePair>,
) : Expression(openBracketToken)

internal class KeyValuePair(
    val keyExpression: Expression,
    val colonToken: Token.Colon,
    val valueExpression: Expression,
)
