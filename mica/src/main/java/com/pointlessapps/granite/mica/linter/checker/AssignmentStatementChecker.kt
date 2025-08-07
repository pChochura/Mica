package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver

internal class AssignmentStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<AssignmentStatement>(scope) {

    // TODO add support for multiple declarations at once

    override fun check(statement: AssignmentStatement) {
        // Check whether the variable is declared
        statement.checkDeclaration()

        // Check whether the expression type is resolvable and matches the variable declaration
        statement.checkExpressionType()
    }

    private fun AssignmentStatement.checkDeclaration() {
        if (!scope.variables.containsKey(lhsToken.value)) {
            scope.addError(
                message = "Variable ${lhsToken.value} is not declared",
                token = startingToken,
            )
        }
    }

    private fun AssignmentStatement.checkExpressionType() {
        val expressionType = typeResolver.resolveExpressionType(rhs)
        val variable = scope.variables[lhsToken.value]
        if (variable != null && variable.type != null && !expressionType.canBeCoercedTo(variable.type)) {
            scope.addError(
                message = "Type mismatch: expected ${variable.type.name}, got ${expressionType.name}",
                token = rhs.startingToken,
            )
        }
    }
}
