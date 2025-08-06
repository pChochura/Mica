package com.pointlessapps.granite.mica.semantics.checker

import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.semantics.model.Scope
import com.pointlessapps.granite.mica.semantics.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.semantics.resolver.TypeResolver

internal class VariableDeclarationStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<VariableDeclarationStatement>(scope) {

    override fun check(statement: VariableDeclarationStatement) {
        // Check whether the variable type is defined
        statement.checkType()

        // Check whether the expression type is resolvable
        statement.checkExpressionType()
    }

    private fun VariableDeclarationStatement.checkType() {
        if (type == null) {
            scope.addError(
                message = "Variable type (${typeToken.value}) is not defined",
                token = typeToken,
            )
        }
    }

    private fun VariableDeclarationStatement.checkExpressionType() {
        val expressionType = typeResolver.resolveExpressionType(rhs)
        if (type != null && !expressionType.canBeCoercedTo(type)) {
            scope.addError(
                message = "Type mismatch: expected $type, got $expressionType",
                token = rhs.startingToken,
            )
        }
    }
}
