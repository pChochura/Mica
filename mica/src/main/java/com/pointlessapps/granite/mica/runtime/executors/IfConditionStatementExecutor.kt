package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
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
        val expressionType = typeResolver.resolveExpressionType(statement.conditionExpression)
        if (
            onAnyExpressionCallback(statement.conditionExpression, scope, typeResolver)
                .coerceToType(expressionType, BoolType) as Boolean
        ) {
            val localScope = Scope(
                scopeType = ScopeType.If(statement),
                parent = scope,
            )
            val newTypeResolver = TypeResolver(localScope)
            statement.body.forEach {
                onStatementExecutionCallback(it, localScope, newTypeResolver)
                if (scope.controlFlowBreakValue != null) {
                    return@forEach
                }
            }

            return
        }

        val elseIfBody = statement.elseIfConditionStatements?.firstNotNullOfOrNull {
            val expressionType =
                typeResolver.resolveExpressionType(it.elseIfConditionExpression)
            if (
                onAnyExpressionCallback(it.elseIfConditionExpression, scope, typeResolver)
                    .coerceToType(expressionType, BoolType) as Boolean
            ) {
                it.elseIfBody
            } else {
                null
            }
        }

        if (elseIfBody != null) {
            val localScope = Scope(
                scopeType = ScopeType.If(statement),
                parent = scope,
            )
            val newTypeResolver = TypeResolver(localScope)
            elseIfBody.forEach {
                onStatementExecutionCallback(it, localScope, newTypeResolver)
                if (scope.controlFlowBreakValue != null) {
                    return@forEach
                }
            }

            return
        }

        if (statement.elseStatement != null) {
            val localScope = Scope(
                scopeType = ScopeType.If(statement),
                parent = scope,
            )
            val newTypeResolver = TypeResolver(localScope)
            statement.elseStatement.elseBody.forEach {
                onStatementExecutionCallback(it, localScope, newTypeResolver)
                if (scope.controlFlowBreakValue != null) {
                    return@forEach
                }
            }
        }
    }
}
