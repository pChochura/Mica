package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.UndefinedType

internal class UserOutputCallStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<UserOutputCallStatement>(scope) {

    override fun check(statement: UserOutputCallStatement) {
        // Check whether the expression type is printable
        statement.checkExpressionType()
    }

    private fun UserOutputCallStatement.checkExpressionType() {
        val returnType = typeResolver.resolveExpressionType(contentExpression)
        if (returnType is UndefinedType) {
            scope.addError(
                message = "Type of the expression ($returnType) cannot be printed",
                token = contentExpression.startingToken,
            )
        }
    }
}
