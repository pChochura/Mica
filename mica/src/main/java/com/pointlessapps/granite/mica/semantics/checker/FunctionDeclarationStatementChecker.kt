package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.model.CheckRapport
import com.pointlessapps.granite.mica.semantics.model.ErrorType
import com.pointlessapps.granite.mica.semantics.model.ReportedError
import com.pointlessapps.granite.mica.semantics.model.ReportedWarning
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class FunctionDeclarationStatementChecker(
    private val variables: Map<String, VariableDeclarationStatement>,
    private val typeResolver: TypeResolver,
) : StatementChecker<FunctionDeclarationStatement> {

    // TODO think about a one loop for all of the checks
    override fun check(statement: FunctionDeclarationStatement): CheckRapport {
        val warnings = mutableListOf<ReportedWarning>()
        val errors = mutableListOf<ReportedError>()

        // Check whether the parameter types are resolvable
        statement.checkParameterTypes(warnings, errors)

        // Check whether the return statements have the same type as the return type
        statement.checkReturnType(warnings, errors)

        // Check for statements below the return statement
        statement.checkUnreachableStatements(warnings)

        // Check for nested function declaration
        statement.checkNestedFunctionDeclaration(errors)

        // Check for parameter redeclaration inside of the function
        statement.checkParameterRedeclaration(errors)

        // TODO Check for the nested if return statements

        return CheckRapport(warnings, errors)
    }

    private fun FunctionDeclarationStatement.checkParameterTypes(
        warnings: MutableList<ReportedWarning>,
        errors: MutableList<ReportedError>,
    ) {
        val names = mutableSetOf<String>()

        parameterTypes.forEach {
            val parameterName = it.key.nameToken.value
            if (names.contains(parameterName)) {
                errors.add(
                    ReportedError(
                        message = "Redeclaration of the parameter: $parameterName",
                        token = it.key.nameToken,
                    ),
                )
            }

            if (variables.containsKey(parameterName)) {
                warnings.add(
                    ReportedWarning(
                        message = "The parameter shadows the names of the variable: $parameterName",
                        token = it.key.nameToken,
                    ),
                )
            }

            names.add(parameterName)

            if (it.value == null) {
                errors.add(
                    ReportedError(
                        message = "Parameter type (${it.key.typeToken.value}) is not defined",
                        token = it.key.typeToken,
                    ),
                )
            }
        }
    }

    private fun FunctionDeclarationStatement.checkReturnType(
        warnings: MutableList<ReportedWarning>,
        errors: MutableList<ReportedError>,
    ) {
        if (returnTypeToken != null && returnType == null) {
            errors.add(
                ReportedError(
                    message = "Return type (${returnTypeToken.value}) is not defined",
                    token = returnTypeToken,
                ),
            )
        }

        var encounteredReturnStatement = false
        body.forEach {
            if (it !is ReturnStatement) return@forEach
            encounteredReturnStatement = true

            if (it.returnExpression == null) {
                if (returnTypeToken != null) {
                    errors.add(
                        ReportedError(
                            message = "Missing return value",
                            token = it.startingToken,
                        ),
                    )
                }

                return
            }

            val expressionType = typeResolver.resolveExpressionType(it.returnExpression)
            if (expressionType is ErrorType) {
                errors.add(
                    ReportedError(
                        message = expressionType.message,
                        token = expressionType.token,
                    ),
                )
            }

            if (returnType == null) {
                warnings.add(
                    ReportedWarning(
                        message = "Unused return expression",
                        token = it.returnExpression.startingToken,
                    ),
                )
            } else if (!expressionType.canBeCoercedTo(returnType)) {
                errors.add(
                    ReportedError(
                        message = "Return type mismatch ($expressionType -> $returnType)",
                        token = it.startingToken,
                    ),
                )
            }
        }

        if (returnTypeToken != null && !encounteredReturnStatement) {
            errors.add(
                ReportedError(
                    message = "Missing return statement",
                    token = returnTypeToken,
                ),
            )
        }
    }

    private fun FunctionDeclarationStatement.checkUnreachableStatements(
        warnings: MutableList<ReportedWarning>,
    ) {
        val lastReturnStatementIndex = body.indexOfLast { it is ReturnStatement }
        if (lastReturnStatementIndex == body.lastIndex || lastReturnStatementIndex == -1) return

        body.subList(lastReturnStatementIndex, body.lastIndex).forEach {
            warnings.add(
                ReportedWarning(
                    message = "Unreachable statement after the return statement",
                    token = it.startingToken,
                ),
            )
        }
    }

    private fun FunctionDeclarationStatement.checkNestedFunctionDeclaration(
        errors: MutableList<ReportedError>,
    ) {
        body.forEach {
            if (it !is FunctionDeclarationStatement) return@forEach

            errors.add(
                ReportedError(
                    message = "Nested function declaration is not supported",
                    token = it.nameToken,
                ),
            )
        }
    }

    private fun FunctionDeclarationStatement.checkParameterRedeclaration(
        errors: MutableList<ReportedError>,
    ) {
        val parametersByName = parameters.associateBy { it.nameToken.value }

        body.forEach {
            val name = when (it) {
                is VariableDeclarationStatement -> it.lhsToken.value
                is AssignmentStatement -> it.lhsToken.value
                else -> return@forEach
            }

            if (parametersByName.containsKey(name)) {
                errors.add(
                    ReportedError(
                        message = "Redeclaration of the parameter: $name",
                        token = it.startingToken,
                    ),
                )
            } else if (variables.containsKey(name)) {
                errors.add(
                    ReportedError(
                        message = "Redeclaration of the variable: ${
                            name
                        }. Use the assignment operator instead",
                        token = it.startingToken,
                    ),
                )
            }
        }
    }
}
