package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.StringType
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver

internal class UserOutputCallStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<UserOutputCallStatement>(scope) {

    override fun check(statement: UserOutputCallStatement) {
        // Check whether the expression type is resolvable to String
        statement.checkExpressionType()
    }

    private fun UserOutputCallStatement.checkExpressionType() {
        val returnType = typeResolver.resolveExpressionType(contentExpression)
        if (!returnType.canBeCoercedTo(StringType)) {
            scope.addError(
                message = "Expression type doesn't resolve to a String",
                token = contentExpression.startingToken,
            )
        }
    }
}
