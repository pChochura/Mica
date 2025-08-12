package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.linter.model.ControlFlowBreak
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object ReturnStatementExecutor {

    suspend fun execute(
        statement: ReturnStatement,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression) -> Any,
    ) {
        // Find a function scope
        var currentScope = scope
        while (currentScope.scopeType !is ScopeType.Function) {
            // It was checked by the linter whether the return statement
            // exist in a function scope
            currentScope = requireNotNull(currentScope.parent)
        }

        val functionReturnType = currentScope.scopeType.statement.returnTypeExpression
            ?.let(typeResolver::resolveExpressionType)

        if (functionReturnType == null) {
            // Set the break in a local scope to break the loops as well
            scope.controlFlowBreakValue = ControlFlowBreak.Return(null)
            return
        }

        val expression = requireNotNull(statement.returnExpression)
        // Set the break in a local scope to break the loops as well
        scope.controlFlowBreakValue = ControlFlowBreak.Return(
            onAnyExpressionCallback(expression)
                .coerceToType(
                    originalType = typeResolver.resolveExpressionType(expression),
                    targetType = functionReturnType,
                ),
        )
    }
}
