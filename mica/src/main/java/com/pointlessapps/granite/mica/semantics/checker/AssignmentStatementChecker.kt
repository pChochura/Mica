package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ErrorType
import com.pointlessapps.granite.mica.semantics.model.ReportedError
import com.pointlessapps.granite.mica.semantics.model.ReportedWarning
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class AssignmentStatementChecker(
    private val variables: Map<String, VariableDeclarationStatement>,
    private val typeResolver: TypeResolver,
) : StatementChecker<AssignmentStatement> {

    override fun check(statement: AssignmentStatement): CheckRapport {
        val warnings = mutableListOf<ReportedWarning>()
        val errors = mutableListOf<ReportedError>()

        // Check whether the variable is declared
        statement.checkDeclaration(errors)

        // Check whether the expression type is resolvable and matches the variable declaration
        statement.checkExpressionType(errors)

        return CheckRapport(
            warnings = warnings,
            errors = errors,
        )
    }

    private fun AssignmentStatement.checkDeclaration(
        errors: MutableList<ReportedError>,
    ) {
        if (!variables.containsKey(lhsToken.value)) {
            errors.add(
                ReportedError(
                    message = "Variable ${lhsToken.value} is not declared",
                    token = lhsToken,
                ),
            )
        }
    }

    private fun AssignmentStatement.checkExpressionType(
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
        if (variables[lhsToken.value]?.type != expressionType) {
            errors.add(
                ReportedError(
                    message = "Type mismatch: ${variables[lhsToken.value]?.type} != $expressionType",
                    token = rhs.startingToken,
                ),
            )
        }
    }
}
