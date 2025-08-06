package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ReportedError

internal object ReturnStatementChecker : StatementChecker<ReturnStatement> {
    override fun check(statement: ReturnStatement) = CheckRapport(
        warnings = emptyList(),
        errors = listOf(
            ReportedError(
                message = "Root level return statement is not supported",
                token = statement.startingToken,
            ),
        ),
    )
}
