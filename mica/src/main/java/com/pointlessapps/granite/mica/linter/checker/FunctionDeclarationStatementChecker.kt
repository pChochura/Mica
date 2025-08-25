package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.UndefinedType

internal class FunctionDeclarationStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<FunctionDeclarationStatement>(scope) {

    override fun check(statement: FunctionDeclarationStatement) {
        // Declare the function at the beginning to allow for recursion
        scope.declareFunction(
            startingToken = statement.startingToken,
            name = statement.nameToken.value,
            parameters = statement.parameters.map {
                typeResolver.resolveExpressionType(it.typeExpression)
            },
            returnType = statement.returnTypeExpression
                ?.let(typeResolver::resolveExpressionType) ?: UndefinedType,
            accessType = FunctionOverload.AccessType.GLOBAL_AND_MEMBER,
        )

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
                type = typeResolver.resolveExpressionType(it.typeExpression),
            )
        }
        // Check the correctness of the body
        StatementsChecker(localScope).check(statement.body)
        scope.addReports(localScope.reports)
    }

    private fun FunctionDeclarationStatement.checkParameterTypes() {
        val names = mutableSetOf<String>()

        parameters.forEach {
            val parameterName = it.nameToken.value
            if (names.contains(parameterName)) {
                scope.addError(
                    message = "Redeclaration of the parameter: $parameterName",
                    token = it.nameToken,
                )
            }

            if (scope.containsVariable(parameterName)) {
                scope.addWarning(
                    message = "The parameter shadows the names of the variable: $parameterName",
                    token = it.nameToken,
                )
            }

            names.add(parameterName)

            val type = typeResolver.resolveExpressionType(it.typeExpression)
            if (type is UndefinedType) {
                scope.addError(
                    message = "Parameter type (${type.name}) is not defined",
                    token = it.typeExpression.startingToken,
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

        body.forEach {
            val name = when (it) {
                is VariableDeclarationStatement -> it.lhsToken.value
                is AssignmentStatement -> it.lhsToken.value
                else -> return@forEach
            }

            if (parametersByName.containsKey(name)) {
                scope.addError(
                    message = "Redeclaration of the parameter: $name",
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
