package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ReportedError
import com.pointlessapps.granite.mica.semantics.model.ReportedWarning
import com.pointlessapps.granite.mica.semantics.model.StringType
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo

internal class UserInputCallStatementChecker(
    private val variables: Map<String, VariableDeclarationStatement>,
) : StatementChecker<UserInputCallStatement> {

    override fun check(statement: UserInputCallStatement): CheckRapport {
        val warnings = mutableListOf<ReportedWarning>()
        val errors = mutableListOf<ReportedError>()

        // Check whether the variable is resolvable to String
        statement.checkVariableType(warnings, errors)

        return CheckRapport(
            warnings = warnings,
            errors = errors,
        )
    }

    private fun UserInputCallStatement.checkVariableType(
        warnings: MutableList<ReportedWarning>,
        errors: MutableList<ReportedError>,
    ) {
        val variable = variables[contentToken.value]
        val variableType = variable?.type
        if (variable == null || variableType == null) {
            errors.add(
                ReportedError(
                    message = "Variable ${contentToken.value} is not declared",
                    token = contentToken,
                ),
            )

            return
        }

        if (variableType.canBeCoercedTo(StringType)) {
            errors.add(
                ReportedError(
                    message = "Variable ${contentToken.value} type doesn't resolve to a String",
                    token = contentToken,
                ),
            )
        } else if (variableType !is StringType) {
            warnings.add(
                ReportedWarning(
                    message = "Variable ${
                        contentToken.value
                    } is not a String. The input will be coerced to a String",
                    token = contentToken,
                ),
            )
        }
    }
}
