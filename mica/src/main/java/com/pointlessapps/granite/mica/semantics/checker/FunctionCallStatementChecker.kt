package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ErrorType
import com.pointlessapps.granite.mica.semantics.model.ReportedError
import com.pointlessapps.granite.mica.semantics.model.ReportedWarning
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class FunctionCallStatementChecker(
    private val typeResolver: TypeResolver,
) : StatementChecker<FunctionCallStatement> {

    override fun check(statement: FunctionCallStatement): CheckRapport {
        val warnings = mutableListOf<ReportedWarning>()
        val errors = mutableListOf<ReportedError>()

        // Check whether the return type is defined and whether the signature exists
        statement.checkReturnType(errors)

        // Check whether the arguments are resolvable
        statement.checkArgumentTypes(errors)

        return CheckRapport(
            warnings = warnings,
            errors = errors,
        )
    }

    private fun FunctionCallStatement.checkReturnType(
        errors: MutableList<ReportedError>,
    ) {
        val returnType = typeResolver.resolveExpressionType(functionCallExpression)
        if (returnType is ErrorType) {
            errors.add(
                ReportedError(
                    message = returnType.message,
                    token = returnType.token,
                ),
            )
        }
    }

    private fun FunctionCallStatement.checkArgumentTypes(
        errors: MutableList<ReportedError>,
    ) {
        functionCallExpression.arguments.forEach {
            val argumentType = typeResolver.resolveExpressionType(it)
            if (argumentType is ErrorType) {
                errors.add(
                    ReportedError(
                        message = argumentType.message,
                        token = argumentType.token,
                    ),
                )
            }
        }
    }
}
