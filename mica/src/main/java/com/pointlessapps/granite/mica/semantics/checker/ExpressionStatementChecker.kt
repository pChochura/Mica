package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ErrorType
import com.pointlessapps.granite.mica.semantics.model.ReportedError
import com.pointlessapps.granite.mica.semantics.model.ReportedWarning
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class ExpressionStatementChecker(
    private val typeResolver: TypeResolver,
) : StatementChecker<ExpressionStatement> {

    override fun check(statement: ExpressionStatement): CheckRapport {
        val warnings = mutableListOf<ReportedWarning>()
        val errors = mutableListOf<ReportedError>()

        // Check whether the expression type is resolvable
        statement.checkExpressionType(errors)

        return CheckRapport(
            warnings = warnings,
            errors = errors,
        )
    }

    private fun ExpressionStatement.checkExpressionType(
        errors: MutableList<ReportedError>,
    ) {
        val expressionType = typeResolver.resolveExpressionType(expression)
        if (expressionType is ErrorType) {
            errors.add(
                ReportedError(
                    message = expressionType.message,
                    token = expressionType.token,
                ),
            )
        }
    }
}
