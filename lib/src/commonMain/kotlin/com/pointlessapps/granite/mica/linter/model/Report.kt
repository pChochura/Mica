package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.model.Token

data class Report(
    val type: ReportType,
    val message: String,
    val token: Token,
) : Comparable<Report> {

    override fun compareTo(other: Report) = compareValuesBy(this, other) { it.token.location }

    fun formatAsString() =
        "${token.location.line}:${token.location.column}: ${type.reportingName}: $message"

    enum class ReportType(val reportingName: String) { ERROR("ERROR"), WARNING("WARN") }
}
