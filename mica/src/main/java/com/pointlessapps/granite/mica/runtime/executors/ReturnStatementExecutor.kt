package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object ReturnStatementExecutor {

    fun execute(
        statement: ReturnStatement,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ) {
        // Find a function scope
        var currentScope = scope
        while (currentScope.scopeType !is ScopeType.Function) {
            // It was checked by the linter whether the return statement
            // exist in a function scope
            currentScope = requireNotNull(currentScope.parent)
        }

        val functionReturnType = currentScope.scopeType.statement.returnType

        if (functionReturnType == null) {
            currentScope.controlFlowBreakValue = Any()
            return
        }

        val expression = requireNotNull(statement.returnExpression)
        currentScope.controlFlowBreakValue =
            onAnyExpressionCallback(expression)
                .coerceToType(
                    originalType = typeResolver.resolveExpressionType(expression),
                    targetType = functionReturnType,
                )
    }
}
