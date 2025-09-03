package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.Root
import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SetTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.builtins.BuiltinFunctionDeclaration
import com.pointlessapps.granite.mica.builtins.builtinFunctions
import com.pointlessapps.granite.mica.helper.getMatchingFunctionDeclaration
import com.pointlessapps.granite.mica.linter.mapper.toType
import com.pointlessapps.granite.mica.linter.model.FunctionOverload
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.BoolType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.StringType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.executors.ArrayIndexGetExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ArrayIndexSetExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ArrayLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.BinaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.CreateCustomObjectExecutor
import com.pointlessapps.granite.mica.runtime.executors.CustomObjectPropertyAccessExecutor
import com.pointlessapps.granite.mica.runtime.executors.PrefixUnaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.SetLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.helper.CustomObject
import com.pointlessapps.granite.mica.runtime.helper.toIntNumber
import com.pointlessapps.granite.mica.runtime.helper.toRealNumber
import com.pointlessapps.granite.mica.runtime.model.BoolVariable
import com.pointlessapps.granite.mica.runtime.model.CharVariable
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteArrayLengthExpression
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecuteCustomObjectPropertyAccessExpression
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
    private val functionDeclarations = builtinFunctions.groupingBy { it.name to it.parameters.size }
        .aggregate { _, acc: MutableMap<List<FunctionOverload.Parameter>, FunctionDefinition>?, element, first ->
            val overload = FunctionDefinition.BuiltinFunction(element)
            if (first) {
                mutableMapOf(element.parameters to overload)
            } else {
                requireNotNull(acc).apply { put(element.parameters, overload) }
            }
        }.toMutableMap()

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
            Instruction.RestoreToStack -> stack.add(
                requireNotNull(
                    value = savedFromStack,
                    lazyMessage = { "No value to restore" },
                ),
            )

            Instruction.SaveFromStack -> savedFromStack = requireNotNull(
                value = stack.lastOrNull(),
                lazyMessage = { "No value to save" },
            )

            is Instruction.Jump -> index = instruction.index - 1
            is Instruction.JumpIf -> executeJumpIf(instruction)
            is Instruction.ExecuteTypeCoercionExpression -> executeTypeCoercionExpression()
            is Instruction.ExecuteTypeExpression -> executeTypeExpression(instruction)
            is Instruction.ExecuteExpression -> executeExpression(instruction)
            is Instruction.ExecuteArrayIndexGetExpression ->
                executeArrayIndexGetExpression(instruction)

            is Instruction.ExecuteArrayIndexSetExpression ->
                executeArrayIndexSetExpression(instruction)

            ExecuteArrayLengthExpression -> executeArrayLengthExpression()

            is Instruction.ExecuteArrayLiteralExpression ->
                executeArrayLiteralExpression(instruction)

            is Instruction.ExecuteSetLiteralExpression ->
                executeSetLiteralExpression(instruction)

            is Instruction.ExecuteUnaryOperation -> executeUnaryOperation(instruction)
            is Instruction.ExecuteBinaryOperation -> executeBinaryOperation(instruction)
            is Instruction.ExecuteFunctionCallExpression ->
                executeFunctionCallExpression(instruction)

            is ExecuteCustomObjectPropertyAccessExpression ->
                executeCustomObjectPropertyAccess(instruction)

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

    @Suppress("UNCHECKED_CAST")
    private fun executeDeclareCustomObjectProperties() {
        val customValue = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "Custom object was not provided" },
        ).value as CustomObject
        customValue.forEach { (name, variable) ->
            variableScope.declarePropertyAlias(
                name = name,
                variable = variable,
                onValueChangedCallback = { customValue[name] = it },
            )
        }
    }

    private fun executeCreateCustomObject(instruction: Instruction.CreateCustomObject) {
        stack.add(
            CreateCustomObjectExecutor.execute(
                types = (1..instruction.propertyNames.size).map {
                    requireNotNull(
                        value = stack.removeLastOrNull(),
                        lazyMessage = { "Property type $it was not provided" },
                    ).type
                }.asReversed(),
                values = (1..instruction.propertyNames.size).map {
                    requireNotNull(
                        value = stack.removeLastOrNull()?.value,
                        lazyMessage = { "Property value $it was not provided" },
                    )
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
            requireNotNull(
                value = stack.removeLastOrNull(),
                lazyMessage = { "Parameter $it type was not provided" },
            ).type
        }.map { FunctionOverload.Parameter(it, Resolver.SUBTYPE_MATCH) }

        functionDeclarations.getOrPut(
            key = instruction.functionName to types.size,
            defaultValue = ::mutableMapOf,
        )[types] = FunctionDefinition.Function(instruction.index)
    }

    private fun executeDeclareVariable(instruction: Instruction.DeclareVariable) {
        val type = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "Variable type was not provided" },
        ).type
        val expressionResult = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "Value to assign was not provided" },
        )
        variableScope.declare(
            name = instruction.variableName,
            value = requireNotNull(
                value = expressionResult.value,
                lazyMessage = { "Value to assign was not provided" },
            ),
            valueType = expressionResult.type,
            variableType = type,
        )
    }

    private fun executeAssignVariable(instruction: Instruction.AssignVariable) {
        val expressionResult = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "Value to assign was not provided" },
        )
        val value = requireNotNull(
            value = expressionResult.value,
            lazyMessage = { "Value to assign was not provided" },
        )
        if (variableScope.get(instruction.variableName) != null) {
            variableScope.assignValue(
                name = instruction.variableName,
                value = value,
                valueType = expressionResult.type,
            )
        } else {
            variableScope.declare(
                name = instruction.variableName,
                value = value,
                valueType = expressionResult.type,
                variableType = expressionResult.type,
            )
        }
    }

    private fun executeCustomObjectPropertyAccess(
        instruction: ExecuteCustomObjectPropertyAccessExpression,
    ) {
        stack.add(
            CustomObjectPropertyAccessExecutor.execute(
                variable = requireNotNull(
                    value = stack.removeLastOrNull(),
                    lazyMessage = { "Variable to access was not provided" },
                ),
                propertyName = instruction.propertyName,
            ),
        )
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
            repeat(instruction.argumentsCount) { stack.removeLastOrNull() }
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
                rhsValue = requireNotNull(
                    value = stack.removeLastOrNull(),
                    lazyMessage = { "Right value to compare against was not provided" },
                ),
                lhsValue = requireNotNull(
                    value = stack.removeLastOrNull(),
                    lazyMessage = { "Left value to compare against was not provided" },
                ),
                operator = instruction.operator,
            ),
        )
    }

    private fun executeUnaryOperation(instruction: Instruction.ExecuteUnaryOperation) {
        stack.add(
            PrefixUnaryOperatorExpressionExecutor.execute(
                value = requireNotNull(
                    value = stack.removeLastOrNull(),
                    lazyMessage = { "Value to operate on was not provided" },
                ),
                operator = instruction.operator,
            ),
        )
    }

    private fun executeSetLiteralExpression(
        instruction: Instruction.ExecuteSetLiteralExpression,
    ) {
        stack.add(
            SetLiteralExpressionExecutor.execute(
                (1..instruction.elementsCount).map {
                    requireNotNull(
                        value = stack.removeLastOrNull(),
                        lazyMessage = { "Set element $it was not provided" },
                    )
                }.asReversed(),
            ),
        )
    }

    private fun executeArrayLiteralExpression(
        instruction: Instruction.ExecuteArrayLiteralExpression,
    ) {
        stack.add(
            ArrayLiteralExpressionExecutor.execute(
                (1..instruction.elementsCount).map {
                    requireNotNull(
                        value = stack.removeLastOrNull(),
                        lazyMessage = { "Array element $it was not provided" },
                    )
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
                    requireNotNull(
                        value = stack.removeLastOrNull(),
                        lazyMessage = { "Array index $it was not provided" },
                    )
                }.asReversed(),
                arrayValue = requireNotNull(
                    value = stack.removeLastOrNull(),
                    lazyMessage = { "Array value was not provided" },
                ),
            ),
        )
    }

    private fun executeArrayIndexSetExpression(
        instruction: Instruction.ExecuteArrayIndexSetExpression,
    ) {
        stack.add(
            ArrayIndexSetExpressionExecutor.execute(
                value = requireNotNull(
                    value = stack.removeLastOrNull(),
                    lazyMessage = { "Value to set was not provided" },
                ),
                arrayIndices = (1..instruction.depth).map {
                    requireNotNull(
                        value = stack.removeLastOrNull(),
                        lazyMessage = { "Array index $it was not provided" },
                    )
                }.asReversed(),
                arrayValue = requireNotNull(
                    value = stack.removeLastOrNull(),
                    lazyMessage = { "Array value was not provided" },
                ),
            ),
        )
    }

    private fun executeArrayLengthExpression() {
        val variable = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "Array value was not provided" },
        )
        val value = variable.type.valueAsSupertype<ArrayType>(variable.value) as List<*>
        stack.add(IntVariable(value.size.toLong()))
    }

    private fun executeJumpIf(instruction: Instruction.JumpIf) {
        val variable = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "Value to compare was not provided" },
        )
        val expressionResult = variable.type.valueAsSupertype<BoolType>(variable.value) as Boolean
        if (expressionResult == instruction.condition) index = instruction.index - 1
    }

    private fun executePrint() {
        val variable = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "Value to print was not provided" },
        )
        onOutputCallback(variable.type.valueAsSupertype<StringType>(variable.value) as String)
    }

    private suspend fun executeAcceptInput() {
        stack.add(StringVariable(onInputCallback()))
    }

    private fun executeReturnFromFunction() {
        index = requireNotNull(
            value = functionCallStack.removeLastOrNull(),
            lazyMessage = { "Function call stack is empty" },
        ) - 1
        variableScopeStack.removeLastOrNull()
    }

    private fun executeTypeCoercionExpression() {
        val type = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "Type to coerce to was not provided" },
        ).type
        val variable = requireNotNull(
            value = stack.removeLastOrNull(),
            lazyMessage = { "Value to coerce was not provided" },
        )
        stack.add(type.toVariable(variable.type.valueAsSupertype(variable.value, type)))
    }

    private fun executeTypeExpression(instruction: Instruction.ExecuteTypeExpression) {
        stack.add(resolveTypeExpression(instruction.expression).toVariable(null))
    }

    private fun resolveTypeExpression(expression: TypeExpression): Type = when (expression) {
        is ArrayTypeExpression -> ArrayType(resolveTypeExpression(expression.typeExpression))
        is SetTypeExpression -> SetType(resolveTypeExpression(expression.typeExpression))
        is SymbolTypeExpression -> expression.symbolToken.toType().takeIf { it != UndefinedType }
            ?: requireNotNull(
                value = typeDeclarations[expression.symbolToken.value],
                lazyMessage = { "Type ${expression.symbolToken.value} was not found" },
            )
    }

    private fun executeExpression(instruction: Instruction.ExecuteExpression) {
        stack.add(
            when (instruction.expression) {
                is SymbolExpression -> requireNotNull(
                    value = variableScope.get(instruction.expression.token.value),
                    lazyMessage = { "Variable ${instruction.expression.token.value} was not found" },
                )

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
