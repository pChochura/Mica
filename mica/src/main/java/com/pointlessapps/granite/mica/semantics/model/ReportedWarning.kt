package com.pointlessapps.granite.mica.semantics.model

import com.pointlessapps.granite.mica.model.Token

internal data class ReportedWarning(val message: String, val token: Token) {
    fun formatAsString() = "${token.location.line}:${token.location.column}: WARN: $message"
}
