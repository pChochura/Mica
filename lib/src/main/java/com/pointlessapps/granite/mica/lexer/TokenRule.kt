package com.pointlessapps.granite.mica.lexer

import com.pointlessapps.granite.mica.lexer.MatchingStrategy.Companion.dataset
import com.pointlessapps.granite.mica.lexer.MatchingStrategy.Companion.regex
import com.pointlessapps.granite.mica.model.Location
import org.intellij.lang.annotations.Language

internal sealed interface MatchingStrategy {
    fun match(input: String, index: Int): String?

    data class WithRegex(val regex: Regex) : MatchingStrategy {
        override fun match(input: String, index: Int) = regex.find(input.substring(index))?.value
    }

    data class WithDataset(val dataset: List<String>) : MatchingStrategy {
        override fun match(input: String, index: Int) =
            dataset.firstOrNull { input.startsWith(it, startIndex = index) }
    }

    companion object {
        fun regex(@Language("RegExp") regex: String) = WithRegex(Regex(regex))
        fun dataset(vararg dataset: String) = WithDataset(dataset.toList())
    }
}

internal sealed class TokenRule(val matchingStrategy: MatchingStrategy) {
    data class Match(
        val location: Location,
        val token: TokenRule,
        val value: String,
    )
}

internal data object SymbolRule : TokenRule(regex("\\A[a-zA-Z_][a-zA-Z0-9_]*"))
internal data object DelimiterRule : TokenRule(
    dataset(
        "..",
        "+=", "-=", "*=", "/=", "%=", "^=", "&=", "|=",
        "++", "--",
        "==", "!=", ">=", "<=",
        "@", ":",
        "(", ")", "[", "]", "{", "}",
        ",", ".",
        "+", "-", "*", "/", "%", "^", "&", "|",
        "=", "!", "<", ">",
    ),
)

internal data object IntNumberRule : TokenRule(regex("\\A[0-9][0-9_]*"))
internal data object RealNumberRule : TokenRule(regex("\\A[0-9][0-9_]*\\.[0-9][0-9_]*"))
internal data object HexNumberRule : TokenRule(regex("\\A0x[0-9a-fA-F]+"))
internal data object BinaryNumberRule : TokenRule(regex("\\A0b[0-1]+"))
internal data object ExponentNumberRule : TokenRule(
    regex("\\A[0-9][0-9_]*(?:\\.[0-9][0-9_]*)?e-?[0-9][0-9_]*"),
)

internal data object CharRule : TokenRule(regex("\\A'(?:[^\\n\\r'\\\\]|\\\\.)'"))
internal data object StringRule : TokenRule(regex("\\A\"(?:[^\\n\\r\"\\\\]|\\\\.)*\""))

internal data object CommentRule : TokenRule(regex("\\A//[^\n]*|\\A/\\*(?s:.)*?\\*/"))
internal data object WhitespaceRule : TokenRule(regex("\\A[ \\t]+"))
internal data object EOLRule : TokenRule(dataset("\n"))

internal data object InterpolatedStringQuote : TokenRule(dataset("\""))
internal data object InterpolatedStringStart : TokenRule(dataset("$("))
internal data object InterpolatedStringEnd : TokenRule(dataset(")"))

internal data object InvalidRule : TokenRule(regex("."))
