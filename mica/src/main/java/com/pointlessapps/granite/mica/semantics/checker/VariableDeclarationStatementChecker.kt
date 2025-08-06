package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ErrorType
import com.pointlessapps.granite.mica.semantics.model.ReportedError
import com.pointlessapps.granite.mica.semantics.model.ReportedWarning
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class VariableDeclarationStatementChecker(
    private val typeResolver: TypeResolver,
) : StatementChecker<VariableDeclarationStatement> {

    override fun check(statement: VariableDeclarationStatement): CheckRapport {
        val warnings = mutableListOf<ReportedWarning>()
        val errors = mutableListOf<ReportedError>()

        // Check whether the variable type is defined
        statement.checkType(errors)

        // Check whether the expression type is resolvable
        statement.checkExpressionType(errors)

        return CheckRapport(
            warnings = warnings,
            errors = errors,
        )
    }

    private fun VariableDeclarationStatement.checkType(
        errors: MutableList<ReportedError>,
    ) {
        if (type == null) {
            errors.add(
                ReportedError(
                    message = "Variable type (${typeToken.value}) is not defined",
                    token = typeToken,
                ),
            )
        }
    }

    private fun VariableDeclarationStatement.checkExpressionType(
        errors: MutableList<ReportedError>,
    ) {
        val expressionType = typeResolver.resolveExpressionType(rhs)
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
