package com.pointlessapps.granite.mica.model

sealed class Token(val location: Location) {
    open class Symbol(location: Location, val value: String) : Token(location)
    class Keyword(location: Location, value: String) : Symbol(location, value)

    class Dollar(location: Location) : Token(location)
    class Colon(location: Location) : Token(location)
    class Comma(location: Location) : Token(location)

    class BracketOpen(location: Location) : Token(location)
    class BracketClose(location: Location) : Token(location)
    class CurlyBracketOpen(location: Location) : Token(location)
    class CurlyBracketClose(location: Location) : Token(location)
    class SquareBracketOpen(location: Location) : Token(location)
    class SquareBracketClose(location: Location) : Token(location)

    class Operator(location: Location, val type: Type) : Token(location) {
        enum class Type {
            Add, Subtract, Multiply, Divide, Exponent,
            Or, And, Not,
            Equals, NotEquals,
            GraterThan, LessThan, GraterThanOrEquals, LessThanOrEquals,
            Range;

            fun valueLiteral() = when (this) {
                Add -> "+"
                Subtract -> "-"
                Multiply -> "*"
                Divide -> "/"
                Exponent -> "^"
                Or -> "|"
                And -> "&"
                Not -> "!"
                Equals -> "=="
                NotEquals -> "!="
                GraterThan -> ">"
                LessThan -> "<"
                GraterThanOrEquals -> ">="
                LessThanOrEquals -> "<="
                Range -> ".."
            }
        }
    }

    class Equals(location: Location) : Token(location)

    class StringLiteral(location: Location, val value: String) : Token(location)
    class BooleanLiteral(location: Location, val value: String) : Token(location)
    class NumberLiteral(location: Location, val value: String, val type: Type) : Token(location) {
        enum class Type { Decimal, Hex, Binary, Exponent }
    }

    class Comment(location: Location, val value: String) : Token(location)

    class Whitespace(location: Location, val value: String) : Token(location)
    class EOL(location: Location) : Token(location)
    class EOF : Token(Location.EMPTY)

    class Invalid(location: Location, val value: String) : Token(location)
}
