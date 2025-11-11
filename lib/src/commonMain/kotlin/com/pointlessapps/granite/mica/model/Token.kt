package com.pointlessapps.granite.mica.model

import kotlin.reflect.KClass

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
            Modulo("%"),
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

            Range(".."),
        }
    }

    class Equals(location: Location) : Token(location)
    class Increment(location: Location) : Token(location)
    class Decrement(location: Location) : Token(location)

    class AssignmentOperator(location: Location, val type: Type) : Token(location) {
        enum class Type(val literal: String) {
            PlusEquals("+="),
            MinusEquals("-="),
            MultiplyEquals("*="),
            DivideEquals("/="),
            ModuloEquals("%="),
            ExponentEquals("^="),

            OrEquals("|="),
            AndEquals("&="),
        }
    }

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
        is Operator -> this.type.literal
        is AssignmentOperator -> this.type.literal
        is NumberLiteral -> when (this.type) {
            NumberLiteral.Type.Int -> "int"
            NumberLiteral.Type.Real -> "real"
            NumberLiteral.Type.Hex -> "hex"
            NumberLiteral.Type.Binary -> "binary"
            NumberLiteral.Type.Exponent -> "exponent"
        }

        else -> print<Token>(this::class)
    }

    companion object {
        inline fun <reified T : Token> print(
            clazz: KClass<out Token> = T::class,
        ): String = when (clazz) {
            Symbol::class -> "Symbol"
            Keyword::class -> "Keyword"
            NumberLiteral::class -> "Number"
            BooleanLiteral::class -> "bool"
            StringLiteral::class -> "string"
            CharLiteral::class -> "char"
            BracketClose::class -> ")"
            BracketOpen::class -> "("
            CurlyBracketClose::class -> "}"
            CurlyBracketOpen::class -> "{"
            SquareBracketClose::class -> "]"
            SquareBracketOpen::class -> "["
            InterpolatedStringQuote::class -> "\""
            InterpolatedStringStart::class -> "$("
            InterpolatedStringEnd::class -> ")"
            Colon::class -> ":"
            Comma::class -> ","
            Dot::class -> "."
            At::class -> "@"
            Equals::class -> "="
            Increment::class -> "++"
            Decrement::class -> "--"
            Invalid::class -> "Invalid"
            Operator::class -> "Operator"
            AssignmentOperator::class -> "Assignment operator"
            Comment::class -> "Comment"
            Whitespace::class -> "Whitespace"
            EOF::class -> "EOF"
            EOL::class -> "EOL"
            else -> "Unknown"
        }
    }
}
