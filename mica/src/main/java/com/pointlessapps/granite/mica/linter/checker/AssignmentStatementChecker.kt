package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.UndefinedType

internal class AssignmentStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<AssignmentStatement>(scope) {

    // TODO add support for multiple declarations at once
    // TODO follow the flow of the program instead of iterating through the statements

    override fun check(statement: AssignmentStatement) {
        // Check whether the variable is declared
        statement.declareIfNecessary()

        // Check whether the expression type is resolvable and matches the variable declaration
        statement.checkExpressionType()
    }

    private fun AssignmentStatement.declareIfNecessary() {
        if (!scope.containsVariable(lhsToken.value)) {
            if (equalSignToken !is Token.Equals) {
                scope.addError(
                    message = "Variable ${lhsToken.value} must be declared before being assigned to",
                    token = startingToken,
                )

                return
            }

            val type = typeResolver.resolveExpressionType(rhs)
            if (type is UndefinedType) {
                scope.addError(
                    message = "Type of the variable ${lhsToken.value} could not be determined",
                    token = rhs.startingToken,
                )

                return
            }

            scope.declareVariable(
                startingToken = startingToken,
                name = lhsToken.value,
                type = type,
            )
        }
    }

    private fun AssignmentStatement.checkExpressionType() {
        val expressionType = typeResolver.resolveExpressionType(rhs)
        val variableType = scope.getVariable(lhsToken.value)
        if (variableType != null && !expressionType.isSubtypeOf(variableType)) {
            scope.addError(
                message = "Type mismatch: expected ${variableType.name}, got ${expressionType.name}",
                token = rhs.startingToken,
            )
        }
    }
}
