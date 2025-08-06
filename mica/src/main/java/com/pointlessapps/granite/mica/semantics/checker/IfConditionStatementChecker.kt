package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.semantics.model.BoolType
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ErrorType
import com.pointlessapps.granite.mica.semantics.model.ReportedError
import com.pointlessapps.granite.mica.semantics.model.ReportedWarning
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class IfConditionStatementChecker(
    private val typeResolver: TypeResolver,
) : StatementChecker<IfConditionStatement> {

    override fun check(statement: IfConditionStatement): CheckRapport {
        val warnings = mutableListOf<ReportedWarning>()
        val errors = mutableListOf<ReportedError>()

        // Check whether the expression type is resolvable to Bool
        statement.checkExpressionType(errors)

        // Check for the root level return statements
        statement.checkReturnStatements(errors)

        // Check the nested if statements
        statement.checkNestedIfStatements(warnings, errors)

        return CheckRapport(
            warnings = warnings,
            errors = errors,
        )
    }

    private fun IfConditionStatement.checkExpressionType(
        errors: MutableList<ReportedError>,
    ) {
        flattenExpressions.forEach {
            val type = typeResolver.resolveExpressionType(it)
            if (type is ErrorType) {
                errors.add(
                    ReportedError(
                        message = type.message,
                        token = type.token,
                    ),
                )
            } else if (!type.canBeCoercedTo(BoolType)) {
                errors.add(
                    ReportedError(
                        message = "Expression type doesn't resolve to a Boolean",
                        token = it.startingToken,
                    ),
                )
            }
        }
    }

    private fun IfConditionStatement.checkReturnStatements(
        errors: MutableList<ReportedError>,
    ) {
        flattenStatements.forEach {
            if (it is ReturnStatement) {
                errors.add(
                    ReportedError(
                        message = "Root level return statement is not supported",
                        token = it.startingToken,
                    ),
                )
            }
        }
    }

    private fun IfConditionStatement.checkNestedIfStatements(
        warnings: MutableList<ReportedWarning>,
        errors: MutableList<ReportedError>,
    ) {
        flattenStatements.forEach {
            if (it is IfConditionStatement) {
                val rapport = check(it)
                warnings.addAll(rapport.warnings)
                errors.addAll(rapport.errors)
            }
        }
    }
}
