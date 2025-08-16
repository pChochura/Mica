package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.ArrayIndexExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
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
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.UnaryExpression
import com.pointlessapps.granite.mica.linter.model.Scope
import com.pointlessapps.granite.mica.linter.model.ScopeType
import com.pointlessapps.granite.mica.linter.resolver.TypeResolver
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.runtime.executors.ArrayIndexExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ArrayLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.BinaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.PrefixUnaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.helper.toNumber
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.State
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class Runtime(private val rootAST: Root) {

    private lateinit var onOutputCallback: (String) -> Unit
    private lateinit var onInputCallback: suspend () -> String

    private lateinit var instructions: List<Instruction>
    private var startingIndex = 0

    private val functionCallStack = mutableListOf<Int>()
    private val stack = mutableListOf<Variable<*>>()

    private var index = 0

    suspend fun execute(
        onOutputCallback: (String) -> Unit,
        onInputCallback: suspend () -> String,
    ) {
        this.onOutputCallback = onOutputCallback
        this.onInputCallback = onInputCallback

        val scope = Scope(scopeType = ScopeType.Root, parent = null)
        val typeResolver = TypeResolver(scope)
        val rootState = State(variables = mutableMapOf(), parent = null)
        AstTraverser.traverse(rootAST).let {
            startingIndex = it.first
            instructions = it.second
        }
        index = startingIndex
        while (index < instructions.size) {
            executeNextInstruction(rootState, typeResolver)
        }
    }

    private suspend fun executeNextInstruction(
        state: State,
        typeResolver: TypeResolver,
    ) {
        when (val instruction = instructions[index]) {
            is Instruction.Label -> {} // NOOP
            Instruction.ReturnFromFunction ->
                index = requireNotNull(functionCallStack.removeLastOrNull()) - 1

            Instruction.AcceptInput -> stack.add(StringType.toVariable(onInputCallback()))
            Instruction.Print -> {
                val output = requireNotNull(stack.removeLastOrNull())
                    .let { it.value?.coerceToType(it.type, StringType) as String }
                onOutputCallback(output)
            }

            is Instruction.Jump -> index = instruction.index - 1
            is Instruction.JumpIfFalse -> {
                val expressionResult = requireNotNull(stack.removeLastOrNull())
                    .let { it.value?.coerceToType(it.type, BoolType) as Boolean }
                if (!expressionResult) index = instruction.index - 1
            }

            is Instruction.ExecuteExpression -> {
                val expressionResult = executeExpression(
                    expression = instruction.expression,
                    state = state,
                    typeResolver = typeResolver,
                )
                stack.add(expressionResult)
            }

            is Instruction.AssignVariable -> {
                val expressionResult = requireNotNull(stack.removeLastOrNull())
                if (state.getVariable(instruction.variableName) != null) {
                    state.assignValue(
                        name = instruction.variableName,
                        value = requireNotNull(expressionResult.value),
                        originalType = expressionResult.type,
                    )
                } else {
                    state.declareVariable(
                        name = instruction.variableName,
                        value = requireNotNull(expressionResult.value),
                        originalType = expressionResult.type,
                        variableType = expressionResult.type,
                    )
                }
            }

            is Instruction.DeclareVariable -> {
                val expressionResult = requireNotNull(stack.removeLastOrNull())
                val type = requireNotNull(stack.removeLastOrNull())
                state.declareVariable(
                    name = instruction.variableName,
                    value = requireNotNull(expressionResult.value),
                    originalType = expressionResult.type,
                    variableType = type.type,
                )
            }
        }

        index++
    }

    private suspend fun executeExpression(
        expression: Expression,
        state: State,
        typeResolver: TypeResolver,
    ): Variable<*> {
        if (!coroutineContext.isActive) throw IllegalStateException("Execution cancelled")

        return when (expression) {
            is SymbolExpression -> requireNotNull(state.getVariable(expression.token.value))
            is CharLiteralExpression -> CharType.toVariable(expression.token.value)
            is StringLiteralExpression -> StringType.toVariable(expression.token.value)
            is BooleanLiteralExpression -> BoolType.toVariable(expression.token.value.toBooleanStrict())
            is NumberLiteralExpression -> NumberType.toVariable(expression.token.value.toNumber())

            is ArrayTypeExpression -> typeResolver.resolveExpressionType(expression)
                .toVariable(emptyList<Any>())

            is SymbolTypeExpression -> typeResolver.resolveExpressionType(expression)
                .toVariable(Any())

            is ArrayIndexExpression -> ArrayIndexExpressionExecutor.execute(
                expression = expression,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, typeResolver) },
            )

            is ArrayLiteralExpression -> ArrayLiteralExpressionExecutor.execute(
                expression = expression,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, typeResolver) },
            )

            is ParenthesisedExpression -> executeExpression(
                expression = expression.expression,
                state = state,
                typeResolver = typeResolver,
            )

            is UnaryExpression -> PrefixUnaryOperatorExpressionExecutor.execute(
                expression = expression,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, typeResolver) },
            )

            is BinaryExpression -> BinaryOperatorExpressionExecutor.execute(
                expression = expression,
                typeResolver = typeResolver,
                onAnyExpressionCallback = { executeExpression(it, state, typeResolver) },
            )

            is FunctionCallExpression -> {
                functionCallStack.add(index)
                expression.arguments.forEach {
                    stack.add(executeExpression(it, state, typeResolver))
                    stack.add(executeExpression(it, state, typeResolver))
                }
                // TODO look for builtin functions first
                val functionIndex = instructions.indexOfFirst {
                    it is Instruction.Label && it.label == expression.nameToken.value
                }
                index = functionIndex
                // TODO just add a functionIndex to the stack and return executing, the return value will be on the stack
                val functionState = State.from(state)
                while (instructions[index] !is Instruction.ReturnFromFunction) {
                    executeNextInstruction(functionState, typeResolver)
                }
                executeNextInstruction(functionState, typeResolver)
                requireNotNull(stack.removeLastOrNull())
            }

            is EmptyExpression ->
                throw IllegalStateException("Such expression should not be evaluated")
        }
    }
}
