package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.builtins.builtinFunctionDeclarations
import com.pointlessapps.granite.mica.helper.getMatchingFunctionDeclaration
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CharType
import com.pointlessapps.granite.mica.model.IntType
import com.pointlessapps.granite.mica.model.RealType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.runtime.executors.ArrayIndexExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ArrayLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.BinaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.PrefixUnaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.helper.toIntNumber
import com.pointlessapps.granite.mica.runtime.helper.toRealNumber
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.model.VariableScope
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class Runtime(private val rootAST: Root) {

    private lateinit var onOutputCallback: (String) -> Unit
    private lateinit var onInputCallback: suspend () -> String

    private lateinit var instructions: List<Instruction>


    private val functionDeclarations =
        mutableMapOf<Pair<String, Int>, MutableMap<List<Type>, Int>>()
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
                val output = requireNotNull(stack.removeLastOrNull()).value as String
                onOutputCallback(output)
            }

            is Instruction.Jump -> index = instruction.index - 1
            is Instruction.JumpIf -> {
                val expressionResult = requireNotNull(stack.removeLastOrNull()).value as Boolean
                if (expressionResult == instruction.condition) index = instruction.index - 1
            }

            is Instruction.ExecuteExpression ->
                executeExpression(instruction.expression, variableScope)

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
                val argumentTypes = arguments.map(Variable<*>::type)

                builtinFunctionDeclarations.getMatchingFunctionDeclaration(
                    name = instruction.functionName,
                    arguments = argumentTypes,
                )?.let {
                    val result = it.execute(
                        arguments.map { arg ->
                            arg.type to requireNotNull(arg.value)
                        },
                    )
                    stack.removeAll(arguments)
                    stack.add(result.first.toVariable(result.second))

                    return
                }

                functionCallStack.add(index + 1)
                // Create a scope from the root state
                variableScopeStack.add(VariableScope.from(variableScopeStack.first()))
                index = requireNotNull(
                    functionDeclarations.getMatchingFunctionDeclaration(
                        name = instruction.functionName,
                        arguments = argumentTypes,
                    ),
                ) - 1
            }

            is Instruction.AssignVariable -> {
                val expressionResult = requireNotNull(stack.removeLastOrNull())
                if (variableScope.get(instruction.variableName) != null) {
                    variableScope.assignValue(
                        name = instruction.variableName,
                        value = requireNotNull(expressionResult.value),
                    )
                } else {
                    variableScope.declare(
                        name = instruction.variableName,
                        value = requireNotNull(expressionResult.value),
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
                    variableType = type,
                )
            }

            is Instruction.DeclareFunction -> {
                val types = (1..instruction.parametersCount).map {
                    requireNotNull(stack.removeLastOrNull()).type
                }.asReversed()

                functionDeclarations.getOrPut(
                    key = instruction.functionName to types.size,
                    defaultValue = ::mutableMapOf,
                )[types] = index + 2
            }

            is Instruction.PushToStack -> stack.add(instruction.value)

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
                is NumberLiteralExpression -> when (expression.token.type) {
                    Token.NumberLiteral.Type.Real, Token.NumberLiteral.Type.Exponent ->
                        RealType.toVariable(expression.token.value.toRealNumber())

                    else -> IntType.toVariable(expression.token.value.toIntNumber())
                }

                else -> throw IllegalStateException("Such expression should not be evaluated")
            },
        )
    }
}
