package com.pointlessapps.granite.mica.model

sealed class Token(val location: Location) {
    open class Symbol(location: Location, val value: String) : Token(location)
    class Keyword(location: Location, value: String) : Symbol(location, value)

    class Dot(location: Location) : Token(location)
    class Colon(location: Location) : Token(location)
    class Comma(location: Location) : Token(location)
    class At(location: Location) : Token(location)

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

    class InterpolatedStringQuote(location: Location) : Token(location)
    class InterpolatedStringStart(location: Location) : Token(location)
    class InterpolatedStringEnd(location: Location) : Token(location)

    class Comment(location: Location, val value: String) : Token(location)

    class Whitespace(location: Location, val value: String) : Token(location)
    class EOL(location: Location) : Token(location)
    class EOF(location: Location) : Token(location)

    class Invalid(location: Location, val value: String) : Token(location)

    override fun toString(): String = when (this) {
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

        is NumberLiteral -> when (this.type) {
            NumberLiteral.Type.Int -> "int"
            NumberLiteral.Type.Real -> "real"
            NumberLiteral.Type.Hex -> "hex"
            NumberLiteral.Type.Binary -> "binary"
            NumberLiteral.Type.Exponent -> "exponent"
        }

        else -> print<Token>(this.javaClass)
    }

    companion object {
        inline fun <reified T : Token> print(
            clazz: Class<T> = T::class.java,
        ): String = when (clazz) {
            BooleanLiteral::class.java -> "bool"
            BracketClose::class.java -> ")"
            BracketOpen::class.java -> "("
            CharLiteral::class.java -> "char"
            Colon::class.java -> ":"
            Comma::class.java -> ","
            At::class.java -> "@"
            Comment::class.java -> "Comment"
            CurlyBracketClose::class.java -> "}"
            CurlyBracketOpen::class.java -> "{"
            Decrement::class.java -> "--"
            Dot::class.java -> "."
            EOF::class.java -> "EOF"
            EOL::class.java -> "EOL"
            Equals::class.java -> "="
            Increment::class.java -> "++"
            Invalid::class.java -> "invalid"
            MinusEquals::class.java -> "-="
            NumberLiteral::class.java -> "number"
            InterpolatedStringQuote::class.java -> "\""
            InterpolatedStringStart::class.java -> "$("
            InterpolatedStringEnd::class.java -> ")"
            Operator::class.java -> "operator"
            PlusEquals::class.java -> "+="
            SquareBracketClose::class.java -> "]"
            SquareBracketOpen::class.java -> "["
            StringLiteral::class.java -> "string"
            Symbol::class.java -> "symbol"
            Whitespace::class.java -> "whitespace"
            else -> "unknown"
        }
    }
}
