package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SetTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
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
import com.pointlessapps.granite.mica.runtime.model.FunctionCall
import com.pointlessapps.granite.mica.runtime.model.FunctionDefinition
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.Instruction.ExecutePropertyAccessExpression
import com.pointlessapps.granite.mica.runtime.model.IntVariable
import com.pointlessapps.granite.mica.runtime.model.RealVariable
import com.pointlessapps.granite.mica.runtime.model.StringVariable
import com.pointlessapps.granite.mica.runtime.model.Variable
import com.pointlessapps.granite.mica.runtime.model.Variable.Companion.toVariable
import com.pointlessapps.granite.mica.runtime.model.VariableScope

@Suppress("UNCHECKED_CAST")
internal fun Runtime.executeDeclareCustomObjectProperties() {
    val customValue = requireNotNull(
        value = stack.removeLastOrNull(),
        lazyMessage = { "Custom object was not provided" },
    ).value as CustomObject
    customValue.keys.forEach { name ->
        variableScope.declarePropertyAlias(
            name = name,
            onVariableCallback = {
                requireNotNull(
                    value = customValue[name],
                    lazyMessage = { "Property $name was not provided" },
                )
            },
            onValueChangedCallback = { customValue[name] = it },
        )
    }
}

internal fun Runtime.executeCreateCustomObject(instruction: Instruction.CreateCustomObject) {
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

internal fun Runtime.executeDeclareType(instruction: Instruction.DeclareType) {
    typeDeclarations[instruction.typeName] = CustomType(instruction.typeName)
}

internal fun Runtime.executeDeclareFunction(instruction: Instruction.DeclareFunction) {
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

internal fun Runtime.executeDeclareVariable(instruction: Instruction.DeclareVariable) {
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

internal fun Runtime.executeAssignVariable(instruction: Instruction.AssignVariable) {
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

internal fun Runtime.executePropertyAccess(
    instruction: ExecutePropertyAccessExpression,
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

internal fun Runtime.executeFunctionCallExpression(
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
        functionCallStack.add(FunctionCall(index + 1, instruction.keepReturnValue))
        // Create a scope from the root state
        variableScopeStack.add(VariableScope.from(variableScopeStack.first()))
        index = functionDefinition.index - 1
    }
}

internal fun Runtime.executeBinaryOperation(instruction: Instruction.ExecuteBinaryOperation) {
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

internal fun Runtime.executeUnaryOperation(instruction: Instruction.ExecuteUnaryOperation) {
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

internal fun Runtime.executeSetLiteralExpression(
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

internal fun Runtime.executeArrayLiteralExpression(
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

internal fun Runtime.executeArrayIndexGetExpression(
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

internal fun Runtime.executeArrayIndexSetExpression(
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

internal fun Runtime.executeArrayLengthExpression() {
    val variable = requireNotNull(
        value = stack.removeLastOrNull(),
        lazyMessage = { "Array value was not provided" },
    )
    val value = variable.type.valueAsSupertype<ArrayType>(variable.value) as List<*>
    stack.add(IntVariable(value.size.toLong()))
}

internal fun Runtime.executeJumpIf(instruction: Instruction.JumpIf) {
    val variable = requireNotNull(
        value = stack.removeLastOrNull(),
        lazyMessage = { "Value to compare was not provided" },
    )
    val expressionResult = variable.type.valueAsSupertype<BoolType>(variable.value) as Boolean
    if (expressionResult == instruction.condition) index = instruction.index - 1
}

internal fun Runtime.executePrint(): String {
    val variable = requireNotNull(
        value = stack.removeLastOrNull(),
        lazyMessage = { "Value to print was not provided" },
    )
    return variable.type.valueAsSupertype<StringType>(variable.value) as String
}

internal suspend fun Runtime.executeAcceptInput(onInputCallback: suspend () -> String) {
    stack.add(StringVariable(onInputCallback()))
}

internal fun Runtime.executeReturnFromFunction() {
    val functionCall = requireNotNull(
        value = functionCallStack.removeLastOrNull(),
        lazyMessage = { "Function call stack is empty" },
    )

    index = functionCall.index - 1
    variableScopeStack.removeLastOrNull()

    if (!functionCall.keepReturnValue) stack.removeLastOrNull()
}

internal fun Runtime.executeTypeCoercionExpression() {
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

internal fun Runtime.executeTypeExpression(instruction: Instruction.ExecuteTypeExpression) {
    stack.add(resolveTypeExpression(instruction.expression).toVariable(null))
}

internal fun Runtime.resolveTypeExpression(expression: TypeExpression): Type = when (expression) {
    is ArrayTypeExpression -> ArrayType(resolveTypeExpression(expression.typeExpression))
    is SetTypeExpression -> SetType(resolveTypeExpression(expression.typeExpression))
    is SymbolTypeExpression -> expression.symbolToken.toType().takeIf { it != UndefinedType }
        ?: requireNotNull(
            value = typeDeclarations[expression.symbolToken.value],
            lazyMessage = { "Type ${expression.symbolToken.value} was not found" },
        )
}

internal fun Runtime.executeExpression(instruction: Instruction.ExecuteExpression) {
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
