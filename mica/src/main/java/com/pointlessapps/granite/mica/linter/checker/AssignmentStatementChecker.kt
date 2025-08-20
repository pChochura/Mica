package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
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
        if (!scope.variables.containsKey(lhsToken.value)) {
            val type = typeResolver.resolveExpressionType(rhs)
            if (type is UndefinedType) return

            scope.declareVariable(
                startingToken = startingToken,
                name = lhsToken.value,
                type = type,
            )
        }
    }

    private fun AssignmentStatement.checkExpressionType() {
        val expressionType = typeResolver.resolveExpressionType(rhs)
        val variableType = scope.variables[lhsToken.value]
        if (variableType != null && expressionType != variableType) {
            scope.addError(
                message = "Type mismatch: expected ${variableType.name}, got ${expressionType.name}",
                token = rhs.startingToken,
            )
        }
    }
}
