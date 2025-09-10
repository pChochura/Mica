package com.pointlessapps.granite.mica.lexer

import com.pointlessapps.granite.mica.model.Location

// TODO rethink the logic (maybe dont use the regex for all cases?)
internal sealed class TokenRule(val regex: Regex) {
    data class Match(
        val location: Location,
        val token: TokenRule,
        val value: String,
    )
}

internal data object SymbolRule : TokenRule(Regex("\\A[a-zA-Z_][a-zA-Z0-9_]*"))
internal data object DelimiterRule : TokenRule(
    Regex("\\A\\.\\.|\\A\\+=|\\A-=|\\A\\+\\+|\\A--|\\A==|\\A!=|\\A>=|\\A<=|\\A[.!<>(){}:,\\-+|&=*/$^\\[\\]]"),
)
internal data object IntNumberRule : TokenRule(Regex("\\A[0-9][0-9_]*"))
internal data object RealNumberRule : TokenRule(Regex("\\A[0-9][0-9_]*\\.[0-9][0-9_]*"))
internal data object HexNumberRule : TokenRule(Regex("\\A0x[0-9a-fA-F]+"))
internal data object BinaryNumberRule : TokenRule(Regex("\\A0b[0-1]+"))
internal data object ExponentNumberRule : TokenRule(Regex("\\A[0-9][0-9_]*(?:\\.[0-9][0-9_]*)?e-?[0-9][0-9_]*"))
internal data object CharRule : TokenRule(Regex("\\A'(?:[^\\n\\r'\\\\]|\\\\.)'"))
internal data object StringRule : TokenRule(Regex("\\A\"(?:[^\\n\\r\"\\\\]|\\\\.)*\""))
internal data object CommentRule : TokenRule(Regex("\\A//[^\n]*|\\A/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL))
internal data object WhitespaceRule : TokenRule(Regex("\\A[ \\t]+"))
internal data object EOLRule : TokenRule(Regex("\\A\n"))

internal data object InterpolatedStringQuote : TokenRule(Regex("\\A\""))
internal data object InterpolatedStringStart : TokenRule(Regex("\\A\\$\\("))
internal data object InterpolatedStringEnd : TokenRule(Regex("\\A\\)"))

internal data object InvalidRule : TokenRule(Regex("."))
