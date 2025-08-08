package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.model.Token

internal class FunctionDeclarationStatementChecker(scope: Scope) :
    StatementChecker<FunctionDeclarationStatement>(scope) {

    override fun check(statement: FunctionDeclarationStatement) {
        // Declare the function at the beginning to allow for recursion
        scope.declareFunction(statement)

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
            localScope.declareVariable(createVariableDeclarationStatement(it))
        }
        // Check the correctness of the body
        StatementsChecker(localScope).check(statement.body)
        scope.addReports(localScope.reports)
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

            if (scope.variables.containsKey(parameterName)) {
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

        val hasReturnStatement = body.find { it is ReturnStatement } != null
        if (returnTypeToken != null && !hasReturnStatement) {
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

    private fun createVariableDeclarationStatement(
        declaration: FunctionParameterDeclarationStatement,
    ): VariableDeclarationStatement = VariableDeclarationStatement(
        lhsToken = declaration.nameToken,
        colonToken = declaration.colonToken,
        typeToken = declaration.typeToken,
        equalSignToken = Token.Equals(declaration.nameToken.location),
        rhs = EmptyExpression,
    )
}
