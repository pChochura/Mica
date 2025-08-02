package com.pointlessapps.granite.mica.ast.expressions

import com.pointlessapps.granite.mica.model.Token

internal sealed class Expression

internal class StringLiteralExpression(val token: Token.StringLiteral) : Expression()
internal class NumberLiteralExpression(val token: Token.NumberLiteral) : Expression()
internal class BooleanLiteralExpression(val token: Token.BooleanLiteral) : Expression()
internal class SymbolExpression(val token: Token.Symbol) : Expression()
internal class ParenthesisedExpression(val expression: Expression) : Expression()
