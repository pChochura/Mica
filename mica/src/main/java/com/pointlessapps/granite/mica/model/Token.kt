package com.pointlessapps.granite.mica.model

sealed class Token(val location: Location) {
    open class Symbol(location: Location, val value: String) : Token(location)
    class Keyword(location: Location, value: String) : Symbol(location, value)

    class Dot(location: Location) : Token(location)
    class Colon(location: Location) : Token(location)
    class Comma(location: Location) : Token(location)

    class BracketOpen(location: Location) : Token(location)
    class BracketClose(location: Location) : Token(location)
    class CurlyBracketOpen(location: Location) : Token(location)
    class CurlyBracketClose(location: Location) : Token(location)
    class SquareBracketOpen(location: Location) : Token(location)
    class SquareBracketClose(location: Location) : Token(location)

    class Operator(location: Location, val type: Type) : Token(location) {
        enum class Type(val literal: String) {
            Add("+"),
            Subtract("-"),
            Multiply("*"),
            Divide("/"),
            Exponent("^"),

            Or("|"),
            And("&"),
            Not("!"),

            Equals("=="),
            NotEquals("!="),

            GraterThan(">"),
            LessThan("<"),
            GraterThanOrEquals(">="),
            LessThanOrEquals("<="),

            Range("..")
        }
    }

    class Equals(location: Location) : Token(location)
    class PlusEquals(location: Location) : Token(location)
    class MinusEquals(location: Location) : Token(location)
    class Increment(location: Location) : Token(location)
    class Decrement(location: Location) : Token(location)

    class CharLiteral(location: Location, val value: Char) : Token(location)
    class StringLiteral(location: Location, val value: String) : Token(location)
    class BooleanLiteral(location: Location, val value: String) : Token(location)
    class NumberLiteral(location: Location, val value: String, val type: Type) : Token(location) {
        enum class Type { Int, Real, Hex, Binary, Exponent }
    }

    class Comment(location: Location, val value: String) : Token(location)

    class Whitespace(location: Location, val value: String) : Token(location)
    class EOL(location: Location) : Token(location)
    class EOF(location: Location) : Token(location)

    class Invalid(location: Location, val value: String) : Token(location)

    override fun toString() = when (this) {
        is BooleanLiteral -> "bool"
        is BracketClose -> ")"
        is BracketOpen -> "("
        is CharLiteral -> "char"
        is Colon -> ":"
        is Comma -> ","
        is Comment -> "Comment"
        is CurlyBracketClose -> "}"
        is CurlyBracketOpen -> "{"
        is Decrement -> "--"
        is Dot -> "."
        is EOF -> "EOF"
        is EOL -> "EOL"
        is Equals -> "="
        is Increment -> "++"
        is Invalid -> "invalid"
        is MinusEquals -> "-="
        is NumberLiteral -> "number"
        is Operator -> when (this.type) {
            Operator.Type.Add -> "+"
            Operator.Type.Subtract -> "-"
            Operator.Type.Multiply -> "*"
            Operator.Type.Divide -> "/"
            Operator.Type.Exponent -> "^"
            Operator.Type.Or -> "|"
            Operator.Type.And -> "&"
            Operator.Type.Not -> "!"
            Operator.Type.Equals -> "=="
            Operator.Type.NotEquals -> "!="
            Operator.Type.GraterThan -> ">"
            Operator.Type.LessThan -> "<"
            Operator.Type.GraterThanOrEquals -> ">="
            Operator.Type.LessThanOrEquals -> "<="
            Operator.Type.Range -> ".."
        }

        is PlusEquals -> "+="
        is SquareBracketClose -> "]"
        is SquareBracketOpen -> "["
        is StringLiteral -> "string"
        is Symbol -> "symbol"
        is Whitespace -> "whitespace"
    }
}
