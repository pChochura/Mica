package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.SymbolDeclarationHelper.declareScope
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.model.ScopeType
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class FunctionDeclarationStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<FunctionDeclarationStatement>(scope) {

    private lateinit var localScope: Scope

    override fun check(statement: FunctionDeclarationStatement) {
        // Takes care of the redeclaration
        localScope = statement.body.declareScope(
            scopeType = ScopeType.Function(statement),
            parentScope = scope,
            allowFunctions = false,
        )
        // Check the correctness of the body
        StatementsChecker(localScope).check(statement.body)
        scope.addReports(localScope.reports)

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
    }

    private fun FunctionDeclarationStatement.checkParameterTypes() {
        val names = mutableSetOf<String>()

        parameterTypes.forEach {
            val parameterName = it.key.nameToken.value
            if (names.contains(parameterName)) {
                scope.addError(
                    message = "Redeclaration of the parameter: $parameterName",
                    token = it.key.nameToken,
                )
            }

            if (localScope.variables.containsKey(parameterName)) {
                scope.addWarning(
                    message = "The parameter shadows the names of the variable: $parameterName",
                    token = it.key.nameToken,
                )
            }

            names.add(parameterName)

            if (it.value == null) {
                scope.addError(
                    message = "Parameter type (${it.key.typeToken.value}) is not defined",
                    token = it.key.typeToken,
                )
            }
        }
    }

    private fun FunctionDeclarationStatement.checkReturnType() {
        if (returnTypeToken != null && returnType == null) {
            scope.addError(
                message = "Return type (${returnTypeToken.value}) is not defined",
                token = returnTypeToken,
            )
        }

        var encounteredReturnStatement = false
        body.forEach {
            if (it !is ReturnStatement) return@forEach
            encounteredReturnStatement = true

            if (it.returnExpression == null) {
                if (returnTypeToken != null) {
                    scope.addError(
                        message = "Missing return value",
                        token = it.startingToken,
                    )
                }

                return
            }

            val expressionType = typeResolver.resolveExpressionType(it.returnExpression)
            if (returnType == null) {
                scope.addWarning(
                    message = "Unused return expression",
                    token = it.returnExpression.startingToken,
                )
            } else if (!expressionType.canBeCoercedTo(returnType)) {
                scope.addError(
                    message = "Return type mismatch: expected $returnType, got $expressionType",
                    token = it.startingToken,
                )
            }
        }

        if (returnTypeToken != null && !encounteredReturnStatement) {
            scope.addError(
                message = "Missing return statement",
                token = returnTypeToken,
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
