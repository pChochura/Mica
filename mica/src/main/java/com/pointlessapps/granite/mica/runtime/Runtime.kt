package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.ArrayIndexExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.BinaryExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.EmptyExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ParenthesisedExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.ast.statements.AssignmentStatement
import com.pointlessapps.granite.mica.ast.statements.BreakStatement
import com.pointlessapps.granite.mica.ast.statements.ExpressionStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionCallStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement
import com.pointlessapps.granite.mica.ast.statements.ReturnStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.UserInputCallStatement
import com.pointlessapps.granite.mica.ast.statements.UserOutputCallStatement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.runtime.executors.ArrayIndexExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ArrayLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.AssignmentStatementExecutor
import com.pointlessapps.granite.mica.runtime.executors.BinaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.BreakStatementExecutor
import com.pointlessapps.granite.mica.runtime.executors.FunctionCallExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.IfConditionStatementExecutor
import com.pointlessapps.granite.mica.runtime.executors.LoopIfStatementExecutor
import com.pointlessapps.granite.mica.runtime.executors.PrefixUnaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ReturnStatementExecutor
import com.pointlessapps.granite.mica.runtime.executors.UserInputCallStatementExecutor
import com.pointlessapps.granite.mica.runtime.executors.VariableDeclarationStatementExecutor
import com.pointlessapps.granite.mica.runtime.helper.toNumber
import com.pointlessapps.granite.mica.runtime.model.State
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class Runtime(private val rootAST: Root) {

    fun execute() {
        val scope = Scope(ScopeType.Root, parent = null)
        val typeResolver = TypeResolver(scope)
        val rootState = State(variables = mutableMapOf(), parent = null)
        rootAST.statements.forEach {
            executeStatement(it, rootState, scope, typeResolver)
        }
    }

    private suspend fun executeStatement(
        statement: Statement,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
    ) {
        if (!coroutineContext.isActive) throw IllegalStateException("Execution cancelled")

        when (statement) {
            is FunctionDeclarationStatement -> scope.declareFunction(statement, typeResolver)
            is VariableDeclarationStatement -> VariableDeclarationStatementExecutor.execute(
                statement = statement,
                state = state,
                scope = scope,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, scope, typeResolver) },
            )

            is AssignmentStatement -> AssignmentStatementExecutor.execute(
                statement = statement,
                state = state,
                scope = scope,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, scope, typeResolver) },
            )

            is BreakStatement -> BreakStatementExecutor.execute(scope)

            is ReturnStatement -> ReturnStatementExecutor.execute(
                statement = statement,
                scope = scope,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, scope, typeResolver) },
            )

            is LoopIfStatement -> LoopIfStatementExecutor.execute(
                statement = statement,
                state = state,
                scope = scope,
                typeResolver = typeResolver,
                onAnyExpressionCallback = ::executeExpression,
                onStatementExecutionCallback = ::executeStatement,
            )

            is IfConditionStatement -> IfConditionStatementExecutor.execute(
                statement = statement,
                state = state,
                scope = scope,
                typeResolver = typeResolver,
                onAnyExpressionCallback = ::executeExpression,
                onStatementExecutionCallback = ::executeStatement,
            )

            is UserInputCallStatement -> UserInputCallStatementExecutor.execute(
                statement = statement,
                state = state,
                scope = scope,
            )

            is UserOutputCallStatement -> {
                val type = typeResolver.resolveExpressionType(statement.contentExpression)
                val output =
                    executeExpression(statement.contentExpression, state, scope, typeResolver)
                        .coerceToType(type, StringType) as String
                println(output)
            }

            is ExpressionStatement ->
                executeExpression(statement.expression, state, scope, typeResolver)

            is FunctionCallStatement ->
                executeExpression(statement.functionCallExpression, state, scope, typeResolver)
        }
    }

    private suspend fun executeExpression(
        expression: Expression,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
    ): Any {
        if (!coroutineContext.isActive) throw IllegalStateException("Execution cancelled")

        return when (expression) {
            is SymbolExpression -> state.getValue(expression.token.value)
            is CharLiteralExpression -> expression.token.value
            is StringLiteralExpression -> expression.token.value
            is BooleanLiteralExpression -> expression.token.value.toBooleanStrict()
            is NumberLiteralExpression -> expression.token.value.toNumber()

            is ArrayIndexExpression -> ArrayIndexExpressionExecutor.execute(
                expression = expression,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, scope, typeResolver) },
            )

            is ArrayLiteralExpression -> ArrayLiteralExpressionExecutor.execute(
                expression = expression,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, scope, typeResolver) },
            )

            is ParenthesisedExpression -> executeExpression(
                expression = expression.expression,
                state = state,
                scope = scope,
                typeResolver = typeResolver,
            )

            is UnaryExpression -> PrefixUnaryOperatorExpressionExecutor.execute(
                expression = expression,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, scope, typeResolver) },
            )

            is BinaryExpression -> BinaryOperatorExpressionExecutor.execute(
                expression = expression,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, scope, typeResolver) },
            )

            is FunctionCallExpression -> FunctionCallExpressionExecutor.execute(
                expression = expression,
                state = state,
                scope = scope,
                typeResolver = typeResolver,
                onAnyExpressionCallback = ::executeExpression,
                onStatementExecutionCallback = ::executeStatement,
            )

            is EmptyExpression, is TypeExpression ->
                throw IllegalStateException("Such expression should not be evaluated")
        }
    }
}
