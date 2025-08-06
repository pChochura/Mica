package com.pointlessapps.granite.mica.semantics.model

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.model.Token

internal data class Scope(
    val parent: Scope? = null,
    val functions: Map<String, FunctionDeclarationStatement>,
    val variables: Map<String, VariableDeclarationStatement>,
) {
    private val reportedWarnings: MutableList<ReportedWarning> = mutableListOf()
    private val reportedErrors: MutableList<ReportedError> = mutableListOf()

    val warnings: List<ReportedWarning>
        get() = reportedWarnings.toList()
    val errors: List<ReportedError>
        get() = reportedErrors.toList()

    fun addErrors(errors: List<ReportedError>) {
        reportedErrors.addAll(errors)
    }

    fun addWarnings(warnings: List<ReportedWarning>) {
        reportedWarnings.addAll(warnings)
    }

    fun addError(message: String, token: Token) {
        reportedErrors.add(ReportedError(message, token))
    }

    fun addWarning(message: String, token: Token) {
        reportedWarnings.add(ReportedWarning(message, token))
    }
}
