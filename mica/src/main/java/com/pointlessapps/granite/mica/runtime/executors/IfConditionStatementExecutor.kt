package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.ElseIfConditionDeclaration
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.linter.model.ControlFlowBreak
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.runtime.model.State
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object IfConditionStatementExecutor {

    suspend fun execute(
        statement: IfConditionStatement,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression, State, Scope, TypeResolver) -> Any,
        onStatementExecutionCallback: suspend (Statement, State, Scope, TypeResolver) -> Unit,
    ) {
        if (
            statement.ifConditionDeclaration.ifConditionExpression.isValueTruthy(
                typeResolver = typeResolver,
                onAnyExpressionCallback = {
                    onAnyExpressionCallback(it, state, scope, typeResolver)
                },
            )
        ) {
            val controlFlowBreak = executeBody(
                state = State.from(state),
                scope = Scope(
                    scopeType = ScopeType.If(statement),
                    parent = scope,
                ),
                statements = statement.ifConditionDeclaration.ifBody,
                onStatementExecutionCallback = onStatementExecutionCallback,
            )

            controlFlowBreak.propagateControlFlowBreakToParent(scope)

            return
        }

        val elseIfBody = findTruthyElseIfBody(
            statements = statement.elseIfConditionDeclarations,
            typeResolver = typeResolver,
            onAnyExpressionCallback = {
                onAnyExpressionCallback(it, state, scope, typeResolver)
            },
        )

        if (elseIfBody != null) {
            executeBody(
                state = State.from(state),
                scope = Scope(
                    scopeType = ScopeType.If(statement),
                    parent = scope,
                ),
                statements = elseIfBody,
                onStatementExecutionCallback = onStatementExecutionCallback,
            ).propagateControlFlowBreakToParent(scope)
        } else if (statement.elseDeclaration != null) {
            executeBody(
                state = State.from(state),
                scope = Scope(
                    scopeType = ScopeType.If(statement),
                    parent = scope,
                ),
                statements = statement.elseDeclaration.elseBody,
                onStatementExecutionCallback = onStatementExecutionCallback,
            ).propagateControlFlowBreakToParent(scope)
        }
    }

    private fun ControlFlowBreak?.propagateControlFlowBreakToParent(scope: Scope) {
        // Propagate only the return control flow break
        if (this != null && this is ControlFlowBreak.Return) {
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

    private suspend fun findTruthyElseIfBody(
        statements: List<ElseIfConditionDeclaration>?,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression) -> Any,
    ): List<Statement>? = statements?.find {
        it.elseIfConditionExpression.isValueTruthy(typeResolver, onAnyExpressionCallback)
    }?.elseIfBody

    private suspend fun Expression.isValueTruthy(
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression) -> Any,
    ): Boolean = onAnyExpressionCallback(this).coerceToType(
        originalType = typeResolver.resolveExpressionType(this),
        targetType = BoolType,
    ) as Boolean
}
