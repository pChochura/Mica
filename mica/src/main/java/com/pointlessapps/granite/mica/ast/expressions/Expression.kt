package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

internal sealed class Expression(val startingToken: Token)

internal class StringLiteralExpression(val token: Token.StringLiteral) : Expression(token)
internal class NumberLiteralExpression(val token: Token.NumberLiteral) : Expression(token)
internal class BooleanLiteralExpression(val token: Token.BooleanLiteral) : Expression(token)
internal class SymbolExpression(val token: Token.Symbol) : Expression(token)
