package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.linter.model.ControlFlowBreak
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.runtime.model.State
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object LoopIfStatementExecutor {

    suspend fun execute(
        statement: LoopIfStatement,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression, State, Scope, TypeResolver) -> Any,
        onStatementExecutionCallback: suspend (Statement, State, Scope, TypeResolver) -> Unit,
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
            val controlFlowBreak = executeBody(
                state = State.from(state),
                scope = Scope(
                    scopeType = ScopeType.LoopIf(statement),
                    parent = scope,
                ),
                statements = statement.ifConditionDeclaration.ifBody,
                onStatementExecutionCallback = onStatementExecutionCallback,
            )
            controlFlowBreak.propagateControlFlowBreakToParent(scope)

            // Break the loop when encountering a return or a break statement
            if (controlFlowBreak != null) return
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
            ).propagateControlFlowBreakToParent(scope)
        }
    }

    private fun ControlFlowBreak?.propagateControlFlowBreakToParent(scope: Scope) {
        // Propagate only the return control flow break
        if (this is ControlFlowBreak.Return) {
            scope.controlFlowBreakValue = this
        }
    }

    private suspend fun executeBody(
        state: State,
        scope: Scope,
        statements: List<Statement>,
        onStatementExecutionCallback: suspend (Statement, State, Scope, TypeResolver) -> Unit,
    ): ControlFlowBreak? {
        val newTypeResolver = TypeResolver(scope)
        statements.forEach {
            onStatementExecutionCallback(it, state, scope, newTypeResolver)
            if (scope.controlFlowBreakValue != null) {
                return scope.controlFlowBreakValue
            }
        }

        return null
    }

    private suspend fun Expression.isValueTruthy(
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression) -> Any,
    ): Boolean = onAnyExpressionCallback(this).coerceToType(
        originalType = typeResolver.resolveExpressionType(this),
        targetType = BoolType,
    ) as Boolean
}
