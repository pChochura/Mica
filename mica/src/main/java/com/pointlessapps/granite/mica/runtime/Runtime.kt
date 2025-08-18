package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.builtins.builtinFunctions
import com.pointlessapps.granite.mica.linter.mapper.toType
import com.pointlessapps.granite.mica.linter.resolver.TypeCoercionResolver.canBeCoercedTo
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.NumberType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.executors.ArrayIndexExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ArrayLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.BinaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.PrefixUnaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.helper.toNumber
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.model.VariableScope
import com.pointlessapps.granite.mica.runtime.resolver.ValueCoercionResolver.coerceToType
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class Runtime(private val rootAST: Root) {

    private lateinit var onOutputCallback: (String) -> Unit
    private lateinit var onInputCallback: suspend () -> String

    private lateinit var instructions: List<Instruction>

    private val functionDeclarations = mutableListOf<Pair<List<Type>, Int>>()
    private val functionCallStack = mutableListOf<Int>()
    private val stack = mutableListOf<Variable<*>>()

    private val variableScopeStack = mutableListOf(VariableScope(mutableMapOf(), null))
    private val variableScope
        get() = variableScopeStack.last()

    private var index = 0

    suspend fun execute(
        onOutputCallback: (String) -> Unit,
        onInputCallback: suspend () -> String,
    ) {
        this.onOutputCallback = onOutputCallback
        this.onInputCallback = onInputCallback

        instructions = AstTraverser.traverse(rootAST)
        while (index < instructions.size) {
            if (!coroutineContext.isActive) return
            executeNextInstruction()
            index++
        }
    }

    // TODO refactor: simplify
    private suspend fun executeNextInstruction() {
        when (val instruction = instructions[index]) {
            is Instruction.Label -> {} // NOOP
            Instruction.ReturnFromFunction -> {
                index = requireNotNull(functionCallStack.removeLastOrNull()) - 1
                variableScopeStack.removeLastOrNull()
            }

            Instruction.AcceptInput -> stack.add(StringType.toVariable(onInputCallback()))
            Instruction.Print -> {
                val output = requireNotNull(stack.removeLastOrNull())
                    .let { it.value?.coerceToType(it.type, StringType) as String }
                onOutputCallback(output)
            }

            is Instruction.Jump -> index = instruction.index - 1
            is Instruction.JumpIf -> {
                val expressionResult = requireNotNull(stack.removeLastOrNull())
                    .let { it.value?.coerceToType(it.type, BoolType) as Boolean }
                if (expressionResult == instruction.condition) index = instruction.index - 1
            }

            is Instruction.ExecuteExpression ->
                executeExpression(instruction.expression, variableScope)

            is Instruction.ExecuteTypeExpression -> stack.add(
                executeTypeExpression(instruction.expression),
            )

            is Instruction.ExecuteArrayIndexExpression -> stack.add(
                ArrayIndexExpressionExecutor.execute(
                    // Reverse order
                    arrayIndex = requireNotNull(stack.removeLastOrNull()),
                    arrayValue = requireNotNull(stack.removeLastOrNull()),
                ),
            )

            is Instruction.ExecuteArrayLiteralExpression -> stack.add(
                ArrayLiteralExpressionExecutor.execute(
                    (1..instruction.elementsCount).map {
                        requireNotNull(stack.removeLastOrNull())
                    }.asReversed(),
                ),
            )

            is Instruction.ExecuteUnaryOperation -> stack.add(
                PrefixUnaryOperatorExpressionExecutor.execute(
                    value = requireNotNull(stack.removeLastOrNull()),
                    operator = instruction.operator,
                ),
            )

            is Instruction.ExecuteBinaryOperation -> stack.add(
                BinaryOperatorExpressionExecutor.execute(
                    // Reverse order
                    rhsValue = requireNotNull(stack.removeLastOrNull()),
                    lhsValue = requireNotNull(stack.removeLastOrNull()),
                    operator = instruction.operator,
                ),
            )

            is Instruction.ExecuteFunctionCallExpression -> {
                val arguments = stack.subList(
                    fromIndex = stack.size - instruction.argumentsCount,
                    toIndex = stack.size,
                )

                builtinFunctions.forEach { builtinFunction ->
                    if (
                        builtinFunction.name != instruction.functionName ||
                        builtinFunction.parameters.size != instruction.argumentsCount
                    ) {
                        return@forEach
                    }

                    val matchesSignature = builtinFunction.parameters
                        .zip(arguments.map(Variable<*>::type))
                        .all { (parameter, argumentType) ->
                            argumentType.canBeCoercedTo(parameter.second)
                        }

                    if (matchesSignature) {
                        // Consume the arguments from the stack
                        val result = builtinFunction.execute(
                            arguments.map { it.type to requireNotNull(it.value) },
                        )
                        stack.removeAll(arguments)
                        stack.add(result.first.toVariable(result.second))

                        return
                    }
                }

                functionCallStack.add(index + 1)
                // Create a scope from the root state
                variableScopeStack.add(VariableScope.from(variableScopeStack.first()))

                var coercedFunctionIndex = -1
                val functionIndex = functionDeclarations.firstNotNullOfOrNull { (types, index) ->
                    if (types.size != arguments.size) {
                        return@firstNotNullOfOrNull null
                    }

                    val pairs = types.zip(arguments.map(Variable<*>::type))
                    if (pairs.all { it.first == it.second }) {
                        return@firstNotNullOfOrNull index
                    }

                    if (
                        coercedFunctionIndex == -1 &&
                        pairs.all { (parameter, argument) ->
                            argument.canBeCoercedTo(parameter)
                        }
                    ) {
                        coercedFunctionIndex = index
                    }

                    return@firstNotNullOfOrNull null
                }

                index = (functionIndex ?: coercedFunctionIndex) - 1
            }

            is Instruction.AssignVariable -> {
                val expressionResult = requireNotNull(stack.removeLastOrNull())
                if (variableScope.get(instruction.variableName) != null) {
                    variableScope.assignValue(
                        name = instruction.variableName,
                        value = requireNotNull(expressionResult.value),
                        originalType = expressionResult.type,
                    )
                } else {
                    variableScope.declare(
                        name = instruction.variableName,
                        value = requireNotNull(expressionResult.value),
                        originalType = expressionResult.type,
                        variableType = expressionResult.type,
                    )
                }
            }

            is Instruction.DeclareVariable -> {
                val type = requireNotNull(stack.removeLastOrNull()).type
                val expressionResult = requireNotNull(stack.removeLastOrNull())
                variableScope.declare(
                    name = instruction.variableName,
                    value = requireNotNull(expressionResult.value),
                    originalType = expressionResult.type,
                    variableType = type,
                )
            }

            is Instruction.DeclareFunction -> {
                val types = (1..instruction.parametersCount).map {
                    requireNotNull(stack.removeLastOrNull()).type
                }.asReversed()
                functionDeclarations.add(types to index + 2)
            }

            Instruction.DeclareScope -> variableScopeStack.add(VariableScope.from(variableScope))
            Instruction.ExitScope -> variableScopeStack.removeLastOrNull()
            Instruction.DuplicateLastStackItem -> stack.add(stack.last())
        }
    }

    private fun executeExpression(expression: Expression, state: VariableScope) {
        stack.add(
            when (expression) {
                is SymbolExpression -> requireNotNull(state.get(expression.token.value))
                is CharLiteralExpression -> CharType.toVariable(expression.token.value)
                is StringLiteralExpression -> StringType.toVariable(expression.token.value)
                is BooleanLiteralExpression -> BoolType.toVariable(expression.token.value.toBooleanStrict())
                is NumberLiteralExpression -> NumberType.toVariable(expression.token.value.toNumber())
                else -> throw IllegalStateException("Such expression should not be evaluated")
            },
        )
    }

    private fun executeTypeExpression(typeExpression: TypeExpression) =
        when (val type = resolveTypeExpressionType(typeExpression)) {
            is ArrayType -> type.toVariable(emptyList<Any>())
            else -> type.toVariable(Any())
        }

    private fun resolveTypeExpressionType(typeExpression: TypeExpression): Type =
        when (typeExpression) {
            is ArrayTypeExpression -> ArrayType(resolveTypeExpressionType(typeExpression.typeExpression))
            is SymbolTypeExpression -> typeExpression.symbolToken.toType() ?: UndefinedType
        }
}
