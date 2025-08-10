package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.StringType

internal class UserOutputCallStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<UserOutputCallStatement>(scope) {

    override fun check(statement: UserOutputCallStatement) {
        // Check whether the expression type is resolvable to string
        statement.checkExpressionType()
    }

    private fun UserOutputCallStatement.checkExpressionType() {
        val returnType = typeResolver.resolveExpressionType(contentExpression)
        if (!returnType.canBeCoercedTo(StringType)) {
            scope.addError(
                message = "Type of the expression (${returnType.name}) doesn't resolve to a string",
                token = contentExpression.startingToken,
            )
        }
    }
}
