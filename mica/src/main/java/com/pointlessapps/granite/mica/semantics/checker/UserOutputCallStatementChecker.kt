package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ErrorType
import com.pointlessapps.granite.mica.semantics.model.ReportedError
import com.pointlessapps.granite.mica.semantics.model.ReportedWarning
import com.pointlessapps.granite.mica.semantics.model.StringType
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class UserOutputCallStatementChecker(
    private val typeResolver: TypeResolver,
) : StatementChecker<UserOutputCallStatement> {

    override fun check(statement: UserOutputCallStatement): CheckRapport {
        val warnings = mutableListOf<ReportedWarning>()
        val errors = mutableListOf<ReportedError>()

        // Check whether the expression type is resolvable to String
        statement.checkExpressionType(errors)

        return CheckRapport(
            warnings = warnings,
            errors = errors,
        )
    }

    private fun UserOutputCallStatement.checkExpressionType(
        errors: MutableList<ReportedError>,
    ) {
        val returnType = typeResolver.resolveExpressionType(contentExpression)
        if (returnType is ErrorType) {
            errors.add(
                ReportedError(
                    message = returnType.message,
                    token = returnType.token,
                ),
            )
        }

        if (!returnType.canBeCoercedTo(StringType)) {
            errors.add(
                ReportedError(
                    message = "Expression type doesn't resolve to a String",
                    token = contentExpression.startingToken,
                ),
            )
        }
    }
}
