package com.pointlessapps.granite.mica.semantics.model

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.model.Token

/**
 * Maps the name of the function to the map containing the function overloads
 * associated by their signature.
 */
internal typealias FunctionOverloads = Map<String, Map<String, FunctionDeclarationStatement>>

internal data class Scope(
    val scopeType: ScopeType,
    val parent: Scope?,
    val functions: FunctionOverloads,
    val variables: Map<String, VariableDeclarationStatement>,
) {
    private val _reports: MutableList<Report> = mutableListOf()
    val reports: List<Report>
        get() = _reports.sorted()

    fun addReports(reports: List<Report>) {
        this._reports.addAll(reports)
    }

    fun addError(message: String, token: Token) {
        _reports.add(
            Report(
                type = Report.ReportType.ERROR,
                message = message,
                token = token,
            ),
        )
    }

    fun addWarning(message: String, token: Token) {
        _reports.add(
            Report(
                type = Report.ReportType.WARNING,
                message = message,
                token = token,
            ),
        )
    }
}
