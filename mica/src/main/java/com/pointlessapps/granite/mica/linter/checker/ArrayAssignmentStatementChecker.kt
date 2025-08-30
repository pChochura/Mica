package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.ArrayAssignmentStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.ArrayType

internal class ArrayAssignmentStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<ArrayAssignmentStatement>(scope) {

    override fun check(statement: ArrayAssignmentStatement) {
        // Check whether the variable is declared
        statement.checkIfExists()

        // Check whether the variable is an array-like object at every depth
        // and the expression type is resolvable and matches the declaration
        statement.checkExpressionType()
    }

    private fun ArrayAssignmentStatement.checkIfExists() {
        if (!scope.containsVariable(arraySymbolToken.value)) {
            scope.addError(
                message = "Variable ${arraySymbolToken.value} must be declared before being assigned to",
                token = startingToken,
            )
        }
    }

    private fun ArrayAssignmentStatement.checkExpressionType() {
        val expressionType = typeResolver.resolveExpressionType(rhs)
        var variableType = scope.getVariable(arraySymbolToken.value)

        var i = 0
        while (variableType != null && i < indexExpressions.size) {
            if (variableType !is ArrayType) {
                scope.addError(
                    message = "Type mismatch: expected an array, got ${variableType.name}",
                    token = indexExpressions[i].openBracketToken,
                )

                return
            }

            variableType = variableType.elementType
            i++
        }

        if (variableType != null && !expressionType.isSubtypeOf(variableType)) {
            scope.addError(
                message = "Type mismatch: expected ${variableType.name}, got ${expressionType.name}",
                token = rhs.startingToken,
            )
        }
    }
}
