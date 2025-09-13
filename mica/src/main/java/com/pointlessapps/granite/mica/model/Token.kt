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

        else -> print<Token>(this.javaClass)
    }

    companion object {
        inline fun <reified T : Token> print(
            clazz: Class<T> = T::class.java,
        ): String = when (clazz) {
            Symbol::class.java -> "Symbol"
            Keyword::class.java -> "Keyword"
            NumberLiteral::class.java -> "Number"
            BooleanLiteral::class.java -> "bool"
            StringLiteral::class.java -> "string"
            CharLiteral::class.java -> "char"
            BracketClose::class.java -> ")"
            BracketOpen::class.java -> "("
            CurlyBracketClose::class.java -> "}"
            CurlyBracketOpen::class.java -> "{"
            SquareBracketClose::class.java -> "]"
            SquareBracketOpen::class.java -> "["
            InterpolatedStringQuote::class.java -> "\""
            InterpolatedStringStart::class.java -> "$("
            InterpolatedStringEnd::class.java -> ")"
            Colon::class.java -> ":"
            Comma::class.java -> ","
            Dot::class.java -> "."
            At::class.java -> "@"
            Equals::class.java -> "="
            Increment::class.java -> "++"
            Decrement::class.java -> "--"
            Invalid::class.java -> "Invalid"
            Operator::class.java -> "Operator"
            AssignmentOperator::class.java -> "Assignment operator"
            Comment::class.java -> "Comment"
            Whitespace::class.java -> "Whitespace"
            EOF::class.java -> "EOF"
            EOL::class.java -> "EOL"
            else -> "Unknown"
        }
    }
}
