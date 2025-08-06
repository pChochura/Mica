package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.EmptyStatement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ReportedWarning

internal object EmptyStatementChecker : StatementChecker<EmptyStatement> {
    override fun check(statement: EmptyStatement) = CheckRapport(
        warnings = listOf(ReportedWarning("Empty statement", statement.startingToken)),
        errors = emptyList(),
    )
}
