package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.UndefinedType

internal class ExpressionStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<ExpressionStatement>(scope) {

    // TODO check whether the expression makes sense (for example the number conversion)

    override fun check(statement: ExpressionStatement) {
        // Check whether the expression type is resolvable
        statement.checkExpressionType()
    }

    private fun ExpressionStatement.checkExpressionType() {
        val expressionType = typeResolver.resolveExpressionType(expression)
        if (expressionType != UndefinedType) {
            scope.addWarning(
                message = "Unused expression result",
                token = startingToken,
            )
        }
    }
}
