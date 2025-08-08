package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.runtime.State
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object LoopIfStatementExecutor {

    fun execute(
        statement: LoopIfStatement,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression, State, Scope, TypeResolver) -> Any,
        onStatementExecutionCallback: (Statement, State, Scope, TypeResolver) -> Unit,
    ) {
        var shouldExecuteElseStatement = true
        while (
            statement.ifConditionDeclaration.ifConditionExpression.isValueTruthy(
                typeResolver = typeResolver,
                onAnyExpressionCallback = {
                    onAnyExpressionCallback(it, state, scope, typeResolver)
                },
            )
        ) {
            shouldExecuteElseStatement = false
            executeBody(
                state = State.from(state),
                scope = Scope(
                    scopeType = ScopeType.LoopIf(statement),
                    parent = scope,
                ),
                statements = statement.ifConditionDeclaration.ifBody,
                onStatementExecutionCallback = onStatementExecutionCallback,
            )
        }

        if (shouldExecuteElseStatement && statement.elseDeclaration != null) {
            executeBody(
                state = State.from(state),
                scope = Scope(
                    scopeType = ScopeType.LoopIf(statement),
                    parent = scope,
                ),
                statements = statement.elseDeclaration.elseBody,
                onStatementExecutionCallback = onStatementExecutionCallback,
            )
        }
    }

    private fun executeBody(
        state: State,
        scope: Scope,
        statements: List<Statement>,
        onStatementExecutionCallback: (Statement, State, Scope, TypeResolver) -> Unit,
    ) {
        val newTypeResolver = TypeResolver(scope)
        statements.forEach {
            onStatementExecutionCallback(it, state, scope, newTypeResolver)
            if (scope.controlFlowBreakValue != null) {
                return@forEach
            }
        }
    }

    private fun Expression.isValueTruthy(
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ): Boolean = onAnyExpressionCallback(this).coerceToType(
        originalType = typeResolver.resolveExpressionType(this),
        targetType = BoolType,
    ) as Boolean
}
