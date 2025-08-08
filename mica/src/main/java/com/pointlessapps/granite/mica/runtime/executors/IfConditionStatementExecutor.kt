package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.ElseIfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.runtime.State
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object IfConditionStatementExecutor {

    fun execute(
        statement: IfConditionStatement,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression, State, Scope, TypeResolver) -> Any,
        onStatementExecutionCallback: (Statement, State, Scope, TypeResolver) -> Unit,
    ) {
        if (
            statement.conditionExpression.isValueTruthy(
                typeResolver = typeResolver,
                onAnyExpressionCallback = {
                    onAnyExpressionCallback(it, state, scope, typeResolver)
                },
            )
        ) {
            executeBody(
                state = State.from(state),
                scope = Scope(
                    scopeType = ScopeType.If(statement),
                    parent = scope,
                ),
                statements = statement.body,
                onStatementExecutionCallback = onStatementExecutionCallback,
            )

            return
        }

        val elseIfBody = findTruthyElseIfBody(
            statements = statement.elseIfConditionStatements,
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
            )
        } else if (statement.elseStatement != null) {
            executeBody(
                state = State.from(state),
                scope = Scope(
                    scopeType = ScopeType.If(statement),
                    parent = scope,
                ),
                statements = statement.elseStatement.elseBody,
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

    private fun findTruthyElseIfBody(
        statements: List<ElseIfConditionStatement>?,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ): List<Statement>? = statements?.find {
        it.elseIfConditionExpression.isValueTruthy(typeResolver, onAnyExpressionCallback)
    }?.elseIfBody

    private fun Expression.isValueTruthy(
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression) -> Any,
    ): Boolean = onAnyExpressionCallback(this).coerceToType(
        originalType = typeResolver.resolveExpressionType(this),
        targetType = BoolType,
    ) as Boolean
}
