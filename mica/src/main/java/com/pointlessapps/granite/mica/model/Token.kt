package com.pointlessapps.granite.mica.model

sealed interface Token {
    data class Symbol(val value: String) : Token
    data class Keyword(val value: String) : Token

    data object Dollar : Token
    data object Colon : Token
    data object Comma : Token

    data object BracketOpen : Token
    data object BracketClose : Token
    data object CurlyBracketOpen : Token
    data object CurlyBracketClose : Token
    data object SquareBracketOpen : Token
    data object SquareBracketClose : Token

    data class Operator(val type: Type) : Token {
        enum class Type {
            Add, Subtract, Multiply, Divide, Exponent,
            Or, And, Not,
            Equals, NotEquals,
            GraterThan, LessThan, GraterThanOrEquals, LessThanOrEquals,
            Range,
        }
    }

    data object Equals : Token

    data class StringLiteral(val value: String) : Token
    data class BooleanLiteral(val value: String) : Token
    data class NumberLiteral(val value: String, val type: Type) : Token {
        enum class Type { Decimal, Hex, Binary, Exponent }
    }

    data class Comment(val value: String) : Token

    data class Whitespace(val value: String) : Token
    data object EOL : Token
    data object EOF : Token

    data class Invalid(val value: String) : Token
}

internal val Keywords = listOf("number", "bool", "string", "true", "false", "return", "match", "if", "else")
