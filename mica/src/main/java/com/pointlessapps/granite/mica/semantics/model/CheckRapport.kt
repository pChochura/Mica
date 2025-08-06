package com.pointlessapps.granite.mica.semantics.model

internal data class CheckRapport(
    val warnings: List<ReportedWarning>,
    val errors: List<ReportedError>,
)
