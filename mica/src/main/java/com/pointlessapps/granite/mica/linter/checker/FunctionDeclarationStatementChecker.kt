package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.helper.isTypeParameter
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.EmptyCustomType
import com.pointlessapps.granite.mica.model.UndefinedType

internal class FunctionDeclarationStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<FunctionDeclarationStatement>(scope) {

    override fun check(statement: FunctionDeclarationStatement) {
        // Declare the type parameter constraint as a `type` keyword
        // Do this before declaring the function to allow for referencing
        statement.checkTypeParameterConstraint()

        // Declare the function at the beginning to allow for recursion
        var defaultParametersLeft = statement.parameters.size - (
                statement.parameters
                    .indexOfFirst { it.defaultValueExpression != null }
                    .takeIf { it != -1 } ?: statement.parameters.size
                )

        val isVararg = statement.parameters.lastOrNull()?.varargToken != null
        val parameterTypes = statement.parameters.map {
            typeResolver.resolveExpressionType(it.typeExpression)
        }
        val returnType = statement.returnTypeExpression
            ?.let(typeResolver::resolveExpressionType) ?: UndefinedType
        do {
            scope.declareFunction(
                startingToken = statement.startingToken,
                name = statement.nameToken.value,
                isVararg = isVararg && defaultParametersLeft == 0,
                typeParameterConstraint = statement.typeParameterConstraint?.let {
                    typeResolver.resolveExpressionType(it)
                },
                parameters = parameterTypes.subList(0, parameterTypes.size - defaultParametersLeft),
                returnType = returnType,
                accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
            )
        } while (defaultParametersLeft-- > 0)

        // Check whether the parameter types are resolvable
        statement.checkParameterTypes()

        // Check whether the return statements have the same type as the return type
        statement.checkReturnType()

        // Check for statements below the return statement
        statement.checkUnreachableStatements()

        // Check for parameter redeclaration inside of the function
        statement.checkParameterRedeclaration()

        // Check whether the body is empty
        statement.checkEmptyBody()

        val localScope = Scope(
            scopeType = ScopeType.Function(statement),
            parent = scope,
        )
        // Declare parameters as variables
        statement.parameters.forEach {
            localScope.declareVariable(
                startingToken = it.nameToken,
                name = it.nameToken.value,
                // Provide the typeResolver from the parent to prevent accessing the parameters
                // while declaring the them
                type = typeResolver.resolveExpressionType(it.typeExpression),
            )
        }
        // Check the correctness of the body
        StatementsChecker(localScope).check(statement.body)
        scope.addReports(localScope.reports)
    }

    private fun FunctionDeclarationStatement.checkTypeParameterConstraint() {
        if (typeParameterConstraint == null) return

        scope.declareType(
            startingToken = typeParameterConstraint.startingToken,
            name = EmptyCustomType.name,
            properties = emptyMap(),
        )
    }

    private fun FunctionDeclarationStatement.checkParameterTypes() {
        val names = mutableSetOf<String>()
        var defaultParameterValuesStarted = false

        parameters.forEachIndexed { index, parameter ->
            val parameterName = parameter.nameToken.value
            if (names.contains(parameterName)) {
                scope.addError(
                    message = "Redeclaration of the parameter: $parameterName",
                    token = parameter.nameToken,
                )
            }

            if (scope.containsVariable(parameterName)) {
                scope.addWarning(
                    message = "The parameter shadows the names of the variable: $parameterName",
                    token = parameter.nameToken,
                )
            }

            names.add(parameterName)

            val type = typeResolver.resolveExpressionType(parameter.typeExpression)
            if (type is UndefinedType) {
                scope.addError(
                    message = "Parameter type (${type.name}) is not defined",
                    token = parameter.typeExpression.startingToken,
                )
            }

            if (parameter.varargToken != null) {
                if (type !is ArrayType) {
                    scope.addError(
                        message = "Vararg parameter type (${type.name}) is not an array",
                        token = parameter.varargToken,
                    )
                }

                if (index != parameters.lastIndex) {
                    scope.addError(
                        message = "Vararg parameter is allowed only at the end of the parameter list",
                        token = parameter.varargToken,
                    )
                }
            }

            if (parameter.defaultValueExpression != null) {
                defaultParameterValuesStarted = true

                val defaultValueType =
                    typeResolver.resolveExpressionType(parameter.defaultValueExpression)
                if (type.isTypeParameter()) {
                    scope.addError(
                        message = "Default parameter values are allowed only for the concrete types",
                        token = parameter.nameToken,
                    )
                } else if (!defaultValueType.isSubtypeOf(type)) {
                    scope.addError(
                        message = "Parameter default value type (${
                            defaultValueType.name
                        }) does not match the parameter type (${type.name})",
                        token = parameter.defaultValueExpression.startingToken,
                    )
                }
            } else if (defaultParameterValuesStarted) {
                scope.addError(
                    message = "Default parameter values are allowed only at the end of the parameter list",
                    token = parameter.nameToken,
                )
            }
        }
    }

    private fun FunctionDeclarationStatement.checkReturnType() {
        val type = returnTypeExpression?.let(typeResolver::resolveExpressionType)
        if (type != null && type is UndefinedType) {
            scope.addError(
                message = "Return type (${type.name}) is not defined",
                token = returnTypeExpression.startingToken,
            )
        }

        val hasReturnStatement = body.find { it is ReturnStatement } != null
        if (returnTypeExpression != null && !hasReturnStatement) {
            scope.addError(
                message = "Missing return statement",
                token = returnTypeExpression.startingToken,
            )
        }
    }

    private fun FunctionDeclarationStatement.checkUnreachableStatements() {
        val lastReturnStatementIndex = body.indexOfLast { it is ReturnStatement }
        if (lastReturnStatementIndex == body.lastIndex || lastReturnStatementIndex == -1) return

        body.subList(lastReturnStatementIndex, body.lastIndex).forEach {
            scope.addWarning(
                message = "Unreachable statement after the return statement",
                token = it.startingToken,
            )
        }
    }

    private fun FunctionDeclarationStatement.checkParameterRedeclaration() {
        val parametersByName = parameters.associateBy { it.nameToken.value }

        body.filterIsInstance<VariableDeclarationStatement>().forEach {
            if (parametersByName.containsKey(it.lhsToken.value)) {
                scope.addError(
                    message = "Redeclaration of the parameter: ${it.lhsToken.value}",
                    token = it.startingToken,
                )
            }
        }
    }

    private fun FunctionDeclarationStatement.checkEmptyBody() {
        if (body.isEmpty()) {
            scope.addWarning(
                message = "Function body is empty",
                token = startingToken,
            )
        }
    }
}
