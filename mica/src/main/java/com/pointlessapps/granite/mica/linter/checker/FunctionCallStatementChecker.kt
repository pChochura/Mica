package com.pointlessapps.granite.mica.linter.checker

import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.UndefinedType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver

internal class FunctionCallStatementChecker(
    scope: Scope,
    private val typeResolver: TypeResolver,
) : StatementChecker<FunctionCallStatement>(scope) {

    override fun check(statement: FunctionCallStatement) {
        // Check whether the return type is defined and whether the signature exists
        statement.checkReturnType()

        // Check whether the arguments are resolvable
        statement.checkArgumentTypes()
    }

    private fun FunctionCallStatement.checkReturnType() {
        val returnType = typeResolver.resolveExpressionType(functionCallExpression)
        if (returnType != UndefinedType) {
            scope.addWarning(
                message = "Unused return value",
                token = startingToken,
            )
        }
    }

    private fun FunctionCallStatement.checkArgumentTypes() {
        functionCallExpression.arguments.forEach {
            typeResolver.resolveExpressionType(it)
        }
    }
}
