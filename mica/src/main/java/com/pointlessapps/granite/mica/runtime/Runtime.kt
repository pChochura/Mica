package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.builtins.BuiltinFunctionDeclaration
import com.pointlessapps.granite.mica.builtins.builtinFunctionDeclarations
import com.pointlessapps.granite.mica.helper.getMatchingFunctionDeclaration
import com.pointlessapps.granite.mica.linter.mapper.toType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.executors.ArrayIndexGetExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ArrayIndexSetExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ArrayLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.BinaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.CreateCustomObjectExecutor
import com.pointlessapps.granite.mica.runtime.executors.PrefixUnaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.helper.toIntNumber
import com.pointlessapps.granite.mica.runtime.helper.toRealNumber
import com.pointlessapps.granite.mica.runtime.model.BoolVariable
import com.pointlessapps.granite.mica.runtime.model.CharVariable
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.IntVariable
import com.pointlessapps.granite.mica.runtime.model.RealVariable
import com.pointlessapps.granite.mica.runtime.model.StringVariable
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.model.VariableScope
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

internal class Runtime(private val rootAST: Root) {

    sealed interface FunctionDefinition {
        data class Function(val index: Int) : FunctionDefinition
        data class BuiltinFunction(val declaration: BuiltinFunctionDeclaration) :
            FunctionDefinition
    }

    private lateinit var onOutputCallback: (String) -> Unit
    private lateinit var onInputCallback: suspend () -> String

    private lateinit var instructions: List<Instruction>

    private val typeDeclarations = mutableMapOf<String, Type>()
    private val functionDeclarations =
        mutableMapOf<Pair<String, Int>, MutableMap<List<Type>, FunctionDefinition>>().apply {
            putAll(
                builtinFunctionDeclarations.mapValues { (_, v) ->
                    v.mapValues { FunctionDefinition.BuiltinFunction(it.value) }.toMutableMap()
                },
            )
        }

    private val functionCallStack = mutableListOf<Int>()
    private val stack = mutableListOf<Variable<*>>()
    private var savedFromStack: Variable<*>? = null

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

    private suspend fun executeNextInstruction() {
        when (val instruction = instructions[index]) {
            is Instruction.Label -> {} // NOOP
            Instruction.ReturnFromFunction -> executeReturnFromFunction()
            Instruction.AcceptInput -> executeAcceptInput()
            Instruction.Print -> executePrint()
            is Instruction.CreateCustomObject -> executeCreateCustomObject(instruction)
            Instruction.DeclareCustomObjectProperties -> executeDeclareCustomObjectProperties()
            is Instruction.PushToStack -> stack.add(instruction.value)
            Instruction.RestoreToStack -> stack.add(requireNotNull(savedFromStack))
            Instruction.SaveFromStack -> savedFromStack = stack.last()
            is Instruction.Jump -> index = instruction.index - 1
            is Instruction.JumpIf -> executeJumpIf(instruction)
            is Instruction.ExecuteTypeExpression -> executeTypeExpression(instruction)
            is Instruction.ExecuteExpression -> executeExpression(instruction)
            is Instruction.ExecuteArrayIndexGetExpression ->
                executeArrayIndexGetExpression(instruction)

            is Instruction.ExecuteArrayIndexSetExpression ->
                executeArrayIndexSetExpression(instruction)

            is Instruction.ExecuteArrayLiteralExpression ->
                executeArrayLiteralExpression(instruction)

            is Instruction.ExecuteUnaryOperation -> executeUnaryOperation(instruction)
            is Instruction.ExecuteBinaryOperation -> executeBinaryOperation(instruction)
            is Instruction.ExecuteFunctionCallExpression ->
                executeFunctionCallExpression(instruction)

            is Instruction.AssignVariable -> executeAssignVariable(instruction)
            is Instruction.DeclareVariable -> executeDeclareVariable(instruction)
            is Instruction.DeclareFunction -> executeDeclareFunction(instruction)
            is Instruction.DeclareType -> executeDeclareType(instruction)
            Instruction.DeclareScope -> variableScopeStack.add(VariableScope.from(variableScope))
            Instruction.ExitScope -> variableScopeStack.removeLastOrNull()
            is Instruction.DuplicateLastStackItems -> stack.addAll(
                stack.subList(
                    fromIndex = stack.size - instruction.count,
                    toIndex = stack.size,
                )
            )
        }
    }

    private fun executeDeclareCustomObjectProperties() {
        val customValue = requireNotNull(stack.removeLastOrNull()).value as Map<String, Variable<*>>
        customValue.forEach { (name, variable) ->
            variableScope.declare(
                name = name,
                value = requireNotNull(variable.value),
                valueType = variable.type,
                variableType = variable.type,
            )
        }
    }

    private fun executeCreateCustomObject(instruction: Instruction.CreateCustomObject) {
        stack.add(
            CreateCustomObjectExecutor.execute(
                types = (1..instruction.propertyNames.size).map {
                    requireNotNull(stack.removeLastOrNull()).type
                }.asReversed(),
                values = (1..instruction.propertyNames.size).map {
                    requireNotNull(stack.removeLastOrNull()?.value)
                }.asReversed(),
                propertyNames = instruction.propertyNames,
                typeName = instruction.typeName,
            )
        )
    }

    private fun executeDeclareType(instruction: Instruction.DeclareType) {
        typeDeclarations[instruction.typeName] = CustomType(instruction.typeName)
    }

    private fun executeDeclareFunction(instruction: Instruction.DeclareFunction) {
        val types = (1..instruction.parametersCount).map {
            requireNotNull(stack.removeLastOrNull()).type
        }.asReversed()

        functionDeclarations.getOrPut(
            key = instruction.functionName to types.size,
            defaultValue = ::mutableMapOf,
        )[types] = FunctionDefinition.Function(index + 2)
    }

    private fun executeDeclareVariable(instruction: Instruction.DeclareVariable) {
        val type = requireNotNull(stack.removeLastOrNull()).type
        val expressionResult = requireNotNull(stack.removeLastOrNull())
        variableScope.declare(
            name = instruction.variableName,
            value = requireNotNull(expressionResult.value),
            valueType = expressionResult.type,
            variableType = type,
        )
    }

    private fun executeAssignVariable(instruction: Instruction.AssignVariable) {
        val expressionResult = requireNotNull(stack.removeLastOrNull())
        if (variableScope.get(instruction.variableName) != null) {
            variableScope.assignValue(
                name = instruction.variableName,
                value = requireNotNull(expressionResult.value),
                valueType = expressionResult.type,
            )
        } else {
            variableScope.declare(
                name = instruction.variableName,
                value = requireNotNull(expressionResult.value),
                valueType = expressionResult.type,
                variableType = expressionResult.type,
            )
        }
    }

    private fun executeFunctionCallExpression(
        instruction: Instruction.ExecuteFunctionCallExpression,
    ) {
        val arguments = stack.subList(
            fromIndex = stack.size - instruction.argumentsCount,
            toIndex = stack.size,
        )
        val argumentTypes = arguments.map(Variable<*>::type)
        val functionDefinition = functionDeclarations.getMatchingFunctionDeclaration(
            name = instruction.functionName,
            arguments = argumentTypes,
        )

        if (functionDefinition is FunctionDefinition.BuiltinFunction) {
            val result = functionDefinition.declaration.execute(arguments)
            stack.removeAll(arguments)
            stack.add(result)
        } else if (functionDefinition is FunctionDefinition.Function) {
            functionCallStack.add(index + 1)
            // Create a scope from the root state
            variableScopeStack.add(VariableScope.from(variableScopeStack.first()))
            index = functionDefinition.index - 1
        }
    }

    private fun executeBinaryOperation(instruction: Instruction.ExecuteBinaryOperation) {
        stack.add(
            BinaryOperatorExpressionExecutor.execute(
                // Reverse order
                rhsValue = requireNotNull(stack.removeLastOrNull()),
                lhsValue = requireNotNull(stack.removeLastOrNull()),
                operator = instruction.operator,
            ),
        )
    }

    private fun executeUnaryOperation(instruction: Instruction.ExecuteUnaryOperation) {
        stack.add(
            PrefixUnaryOperatorExpressionExecutor.execute(
                value = requireNotNull(stack.removeLastOrNull()),
                operator = instruction.operator,
            ),
        )
    }

    private fun executeArrayLiteralExpression(
        instruction: Instruction.ExecuteArrayLiteralExpression,
    ) {
        stack.add(
            ArrayLiteralExpressionExecutor.execute(
                (1..instruction.elementsCount).map {
                    requireNotNull(stack.removeLastOrNull())
                }.asReversed(),
            ),
        )
    }

    private fun executeArrayIndexGetExpression(
        instruction: Instruction.ExecuteArrayIndexGetExpression,
    ) {
        stack.add(
            ArrayIndexGetExpressionExecutor.execute(
                arrayIndices = (1..instruction.depth).map {
                    requireNotNull(stack.removeLastOrNull())
                }.asReversed(),
                arrayValue = requireNotNull(stack.removeLastOrNull()),
            ),
        )
    }

    private fun executeArrayIndexSetExpression(
        instruction: Instruction.ExecuteArrayIndexSetExpression,
    ) {
        stack.add(
            ArrayIndexSetExpressionExecutor.execute(
                value = requireNotNull(stack.removeLastOrNull()),
                arrayIndices = (1..instruction.depth).map {
                    requireNotNull(stack.removeLastOrNull())
                }.asReversed(),
                arrayValue = requireNotNull(stack.removeLastOrNull()),
            ),
        )
    }

    private fun executeJumpIf(instruction: Instruction.JumpIf) {
        val variable = requireNotNull(stack.removeLastOrNull())
        val expressionResult = variable.type.valueAsSupertype<BoolType>(variable.value) as Boolean
        if (expressionResult == instruction.condition) index = instruction.index - 1
    }

    private fun executePrint() {
        val variable = requireNotNull(stack.removeLastOrNull())
        onOutputCallback(variable.type.valueAsSupertype<StringType>(variable.value) as String)
    }

    private suspend fun executeAcceptInput() {
        stack.add(StringVariable(onInputCallback()))
    }

    private fun executeReturnFromFunction() {
        index = requireNotNull(functionCallStack.removeLastOrNull()) - 1
        variableScopeStack.removeLastOrNull()
    }

    private fun executeTypeExpression(instruction: Instruction.ExecuteTypeExpression) {
        stack.add(resolveTypeExpression(instruction.expression).toVariable(null))
    }

    private fun resolveTypeExpression(expression: TypeExpression): Type = when (expression) {
        is ArrayTypeExpression -> ArrayType(resolveTypeExpression(expression.typeExpression))
        is SymbolTypeExpression -> expression.symbolToken.toType().takeIf { it != UndefinedType }
            ?: requireNotNull(typeDeclarations[expression.symbolToken.value])
    }

    private fun executeExpression(instruction: Instruction.ExecuteExpression) {
        stack.add(
            when (instruction.expression) {
                is SymbolExpression -> requireNotNull(variableScope.get(instruction.expression.token.value))
                is CharLiteralExpression -> CharVariable(instruction.expression.token.value)
                is StringLiteralExpression -> StringVariable(instruction.expression.token.value)
                is BooleanLiteralExpression -> BoolVariable(instruction.expression.token.value.toBooleanStrict())
                is NumberLiteralExpression -> when (instruction.expression.token.type) {
                    Token.NumberLiteral.Type.Real, Token.NumberLiteral.Type.Exponent ->
                        RealVariable(instruction.expression.token.value.toRealNumber())

                    else -> IntVariable(instruction.expression.token.value.toIntNumber())
                }

                else -> throw IllegalStateException("Such expression should not be evaluated")
            },
        )
    }
}
