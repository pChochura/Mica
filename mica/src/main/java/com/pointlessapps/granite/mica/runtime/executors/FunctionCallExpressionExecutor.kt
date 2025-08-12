package com.pointlessapps.granite.mica.runtime.executors

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.FunctionCallExpression
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.FunctionParameterDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.Statement
import com.pointlessapps.granite.mica.ast.statements.VariableDeclarationStatement
import com.pointlessapps.granite.mica.builtins.BuiltinFunctionDeclaration
import com.pointlessapps.granite.mica.builtins.builtinFunctions
import com.pointlessapps.granite.mica.linter.model.ControlFlowBreak
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.Location
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.runtime.model.State

internal object FunctionCallExpressionExecutor {

    suspend fun execute(
        expression: FunctionCallExpression,
        state: State,
        scope: Scope,
        typeResolver: TypeResolver,
        onAnyExpressionCallback: suspend (Expression, State, Scope, TypeResolver) -> Any,
        onStatementExecutionCallback: suspend (Statement, State, Scope, TypeResolver) -> Unit,
    ): Any {
        // Check for the builtin function
        val builtinFunction = findBuiltinFunction(expression, typeResolver)
        if (builtinFunction != null) {
            return builtinFunction.execute(
                expression.arguments.associate { expression ->
                    typeResolver.resolveExpressionType(expression) to
                            onAnyExpressionCallback(expression, state, scope, typeResolver)
                }.toList(),
            )
        }

        val function = findFunctionDeclaration(expression, scope, typeResolver)
        val localScope = Scope(
            scopeType = ScopeType.Function(function),
            parent = scope,
        )
        val newTypeResolver = TypeResolver(localScope)
        val localState = State.from(state)

        // Declare parameters as variables
        function.parameters.zip(expression.arguments).forEach { (declaration, expression) ->
            val statement = createVariableDeclarationStatement(declaration, expression)
            localScope.declareVariable(statement)

            localState.declareVariable(
                name = statement.lhsToken.value,
                value = onAnyExpressionCallback(
                    statement.rhs,
                    localState,
                    localScope,
                    newTypeResolver,
                ),
                originalType = newTypeResolver.resolveExpressionType(statement.rhs),
                variableType = newTypeResolver.resolveExpressionType(declaration.typeExpression),
            )
        }

        val localReturnValue = function.body.firstNotNullOfOrNull {
            onStatementExecutionCallback(it, localState, localScope, newTypeResolver)
            (localScope.controlFlowBreakValue as? ControlFlowBreak.Return)?.value
        }

        return localReturnValue ?: Any()
    }

    private fun findBuiltinFunction(
        expression: FunctionCallExpression,
        typeResolver: TypeResolver,
    ): BuiltinFunctionDeclaration? = builtinFunctions.firstOrNull {
        if (
            it.name != expression.nameToken.value ||
            it.parameters.size != expression.arguments.size
        ) {
            return@firstOrNull false
        }

        return@firstOrNull it.parameters.zip(expression.arguments)
            .all { (parameter, argument) ->
                val argumentType = typeResolver.resolveExpressionType(argument)
                argumentType.canBeCoercedTo(parameter.second)
            }
    }

    private fun findFunctionDeclaration(
        expression: FunctionCallExpression,
        scope: Scope,
        typeResolver: TypeResolver,
    ): FunctionDeclarationStatement = requireNotNull(scope.functions[expression.nameToken.value])
        .firstNotNullOf {
            if (it.value.parameters.size != expression.arguments.size) {
                return@firstNotNullOf null
            }

            val matchesSignature = it.value.parameters.zip(expression.arguments)
                .all { (parameter, argument) ->
                    val parameterType = typeResolver.resolveExpressionType(parameter.typeExpression)
                    val argumentType = typeResolver.resolveExpressionType(argument)
                    argumentType.canBeCoercedTo(parameterType)
                }

            if (matchesSignature) it.value else null
        }

    private fun createVariableDeclarationStatement(
        declaration: FunctionParameterDeclarationStatement,
        expression: Expression,
    ): VariableDeclarationStatement = VariableDeclarationStatement(
        lhsToken = declaration.nameToken,
        colonToken = declaration.colonToken,
        typeExpression = declaration.typeExpression,
        equalSignToken = Token.Equals(Location.EMPTY),
        rhs = expression,
    )
}
