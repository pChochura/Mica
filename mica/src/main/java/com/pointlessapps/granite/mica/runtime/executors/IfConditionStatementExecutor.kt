package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.statements.ElseIfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType

internal object IfConditionStatementExecutor {

    fun execute(
        statement: IfConditionStatement,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: (Expression, Scope, TypeResolver) -> Any,
        onStatementExecutionCallback: (Statement, Scope, TypeResolver) -> Unit,
    ) {
        if (
            statement.conditionExpression.isValueTruthy(
                typeResolver = typeResolver,
                onAnyExpressionCallback = { onAnyExpressionCallback(it, scope, typeResolver) },
            )
        ) {
            executeBody(
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
            onAnyExpressionCallback = { onAnyExpressionCallback(it, scope, typeResolver) },
        )

        if (elseIfBody != null) {
            executeBody(
                scope = Scope(
                    scopeType = ScopeType.If(statement),
                    parent = scope,
                ),
                statements = elseIfBody,
                onStatementExecutionCallback = onStatementExecutionCallback,
            )
        } else if (statement.elseStatement != null) {
            executeBody(
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
        scope: Scope,
        statements: List<Statement>,
        onStatementExecutionCallback: (Statement, Scope, TypeResolver) -> Unit,
    ) {
        val newTypeResolver = TypeResolver(scope)
        statements.forEach {
            onStatementExecutionCallback(it, scope, newTypeResolver)
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
