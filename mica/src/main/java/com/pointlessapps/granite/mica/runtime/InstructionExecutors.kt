package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.ast.expressions.ArrayTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.BooleanLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.CharLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.MapTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.NumberLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SetTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.StringLiteralExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolExpression
import com.pointlessapps.granite.mica.ast.expressions.SymbolTypeExpression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.helper.getMatchingFunctionDeclaration
import com.pointlessapps.granite.mica.linter.mapper.toType
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Companion.of
import com.pointlessapps.granite.mica.linter.model.FunctionOverload.Parameter.Resolver
import com.pointlessapps.granite.mica.mapper.asArrayType
import com.pointlessapps.granite.mica.mapper.asBoolType
import com.pointlessapps.granite.mica.mapper.asCustomType
import com.pointlessapps.granite.mica.mapper.asStringType
import com.pointlessapps.granite.mica.mapper.asType
import com.pointlessapps.granite.mica.mapper.toType
import com.pointlessapps.granite.mica.model.ArrayType
import com.pointlessapps.granite.mica.model.CustomType
import com.pointlessapps.granite.mica.model.MapType
import com.pointlessapps.granite.mica.model.SetType
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.model.Type
import com.pointlessapps.granite.mica.model.UndefinedType
import com.pointlessapps.granite.mica.runtime.executors.AccessorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.ArrayLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.BinaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.CreateCustomObjectExecutor
import com.pointlessapps.granite.mica.runtime.executors.MapLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.PrefixUnaryOperatorExpressionExecutor
import com.pointlessapps.granite.mica.runtime.executors.SetLiteralExpressionExecutor
import com.pointlessapps.granite.mica.runtime.helper.toIntNumber
import com.pointlessapps.granite.mica.runtime.helper.toRealNumber
import com.pointlessapps.granite.mica.runtime.model.FunctionCall
import com.pointlessapps.granite.mica.runtime.model.FunctionDefinition
import com.pointlessapps.granite.mica.runtime.model.Instruction
import com.pointlessapps.granite.mica.runtime.model.VariableScope
import com.pointlessapps.granite.mica.runtime.model.VariableType

@Suppress("UNCHECKED_CAST")
internal fun Runtime.executeDeclareCustomObjectProperties() {
    val customValue = requireNotNull(
        value = (stack.removeLastOrNull() as? VariableType.Value)?.value.asCustomType(),
        lazyMessage = { "Custom object was not provided" },
    ) as MutableMap<String, VariableType.Value>
    customValue.keys.forEach { name ->
        variableScope.declarePropertyAlias(
            name = name,
            onVariableCallback = {
                requireNotNull(
                    value = VariableType.Value(customValue[name]),
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
                    value = stack.removeLastOrNull() as? VariableType.Type,
                    lazyMessage = { "Property type $it was not provided" },
                ).type
            }.asReversed(),
            values = (1..instruction.propertyNames.size).map {
                requireNotNull(
                    value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                    lazyMessage = { "Property value $it was not provided" },
                )
            }.asReversed(),
            typeName = instruction.typeName,
            propertyNames = instruction.propertyNames,
        ),
    )
}

internal fun Runtime.executeDeclareType(instruction: Instruction.DeclareType) {
    typeDeclarations[instruction.typeName] = CustomType(instruction.typeName)
}

internal fun Runtime.executeDeclareFunction(instruction: Instruction.DeclareFunction) {
    val types = (1..instruction.parametersCount).map {
        requireNotNull(
            value = stack.removeLastOrNull() as? VariableType.Type,
            lazyMessage = { "Parameter $it type was not provided" },
        ).type
    }.mapIndexed { index, parameter ->
        Resolver.SUBTYPE_MATCH.of(
            type = parameter,
            vararg = instruction.vararg && index == instruction.parametersCount - 1,
        )
    }

    functionDeclarations.getOrPut(
        key = instruction.functionName,
        defaultValue = ::mutableMapOf,
    )[types] = FunctionDefinition.Function(
        isVararg = instruction.vararg,
        parametersCount = instruction.parametersCount,
        index = instruction.index,
    )
}

internal fun Runtime.executeDeclareVariable(instruction: Instruction.DeclareVariable) {
    val type = requireNotNull(
        value = stack.removeLastOrNull() as? VariableType.Type,
        lazyMessage = { "Variable type was not provided" },
    ).type
    val expressionResult = requireNotNull(
        value = stack.removeLastOrNull() as? VariableType.Value,
        lazyMessage = { "Value to assign was not provided" },
    ).value
    variableScope.declare(
        name = instruction.variableName,
        value = requireNotNull(
            value = expressionResult,
            lazyMessage = { "Value to assign was not provided" },
        ),
        variableType = type,
    )
}

internal fun Runtime.executeAssignVariable(instruction: Instruction.AssignVariable) {
    val expressionResult = requireNotNull(
        value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
        lazyMessage = { "Value to assign was not provided" },
    )
    val valueType = expressionResult.toType()
    if (variableScope.get(instruction.variableName) != null) {
        variableScope.assignValue(
            name = instruction.variableName,
            value = expressionResult,
        )
    } else {
        variableScope.declare(
            name = instruction.variableName,
            value = expressionResult,
            variableType = valueType,
        )
    }
}

internal fun Runtime.executeFunctionCallExpression(
    instruction: Instruction.ExecuteFunctionCallExpression,
) {
    val typeArgument = if (instruction.hasTypeArgument) {
        requireNotNull(
            value = stack.removeLastOrNull() as? VariableType.Type,
            lazyMessage = { "Type argument was not provided" },
        )
    } else {
        null
    }

    val arguments = stack.subList(
        fromIndex = stack.size - instruction.argumentsCount,
        toIndex = stack.size,
    ).map { it as VariableType.Value }
    val argumentTypes = arguments.map { it.value.toType() }
    val functionDefinition = functionDeclarations.getMatchingFunctionDeclaration(
        name = instruction.functionName,
        arguments = argumentTypes,
    )

    if (functionDefinition is FunctionDefinition.BuiltinFunction) {
        val result = functionDefinition.declaration.execute(typeArgument, arguments)
        repeat(instruction.argumentsCount) { stack.removeLastOrNull() }
        stack.add(result)
    } else if (functionDefinition is FunctionDefinition.Function) {
        functionCallStack.add(FunctionCall(index + 1, instruction.keepReturnValue))
        // Create a scope from the root state
        variableScopeStack.add(VariableScope.from(variableScopeStack.first()))

        // Collapse the arguments into an array if it's vararg
        if (functionDefinition.isVararg) {
            executeArrayLiteralExpression(
                Instruction.ExecuteArrayLiteralExpression(
                    instruction.argumentsCount - functionDefinition.parametersCount + 1,
                ),
            )
        }

        index = functionDefinition.index - 1
    }
}

internal fun Runtime.executeBinaryOperation(instruction: Instruction.ExecuteBinaryOperation) {
    stack.add(
        BinaryOperatorExpressionExecutor.execute(
            // Reverse order
            rhsValue = requireNotNull(
                value = stack.removeLastOrNull() as? VariableType.Value,
                lazyMessage = { "Right value to compare against was not provided" },
            ).value,
            lhsValue = requireNotNull(
                value = stack.removeLastOrNull() as? VariableType.Value,
                lazyMessage = { "Left value to compare against was not provided" },
            ).value,
            operator = instruction.operator,
        ),
    )
}

internal fun Runtime.executeUnaryOperation(instruction: Instruction.ExecuteUnaryOperation) {
    stack.add(
        PrefixUnaryOperatorExpressionExecutor.execute(
            value = requireNotNull(
                value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                lazyMessage = { "Value to operate on was not provided" },
            ),
            operator = instruction.operator,
        ),
    )
}

internal fun Runtime.executeMapLiteralExpression(
    instruction: Instruction.ExecuteMapLiteralExpression,
) {
    stack.add(
        MapLiteralExpressionExecutor.execute(
            (1..instruction.elementsCount).map {
                requireNotNull(
                    value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                    lazyMessage = { "Map element key $it was not provided" },
                ) to requireNotNull(
                    value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                    lazyMessage = { "Map element value $it was not provided" },
                )
            }.asReversed(),
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
                    value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
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
                    value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                    lazyMessage = { "Array element $it was not provided" },
                )
            }.asReversed(),
        ),
    )
}

internal fun Runtime.executeAccessorGetExpression(
    instruction: Instruction.ExecuteAccessorGetExpression,
) {
    stack.add(
        AccessorExpressionExecutor.executeGet(
            accessors = (1..instruction.depth).map {
                requireNotNull(
                    value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                    lazyMessage = { "Accessor value $it was not provided" },
                )
            }.asReversed(),
            variable = requireNotNull(
                value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                lazyMessage = { "Variable value was not provided" },
            ),
        ),
    )
}

internal fun Runtime.executeAccessorSetExpression(
    instruction: Instruction.ExecuteAccessorSetExpression,
) {
    stack.add(
        AccessorExpressionExecutor.executeSet(
            value = requireNotNull(
                value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                lazyMessage = { "Value to set was not provided" },
            ),
            accessors = (1..instruction.depth).map {
                requireNotNull(
                    value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                    lazyMessage = { "Accessor value $it was not provided" },
                )
            }.asReversed(),
            variable = requireNotNull(
                value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
                lazyMessage = { "Variable value was not provided" },
            ),
        ),
    )
}

internal fun Runtime.executeArrayLengthExpression() {
    val value = requireNotNull(
        value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
        lazyMessage = { "Array value was not provided" },
    )
    stack.add(VariableType.Value(value.asArrayType().size.toLong()))
}

internal fun Runtime.executeJumpIf(instruction: Instruction.JumpIf) {
    val variable = requireNotNull(
        value = (stack.removeLastOrNull() as? VariableType.Value?)?.value,
        lazyMessage = { "Value to compare was not provided" },
    )
    if (variable.asBoolType() == instruction.condition) index = instruction.index - 1
}

internal fun Runtime.executePrint(): String {
    val variable = requireNotNull(
        value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
        lazyMessage = { "Value to print was not provided" },
    )
    return variable.asStringType()
}

internal suspend fun Runtime.executeAcceptInput(onInputCallback: suspend () -> String) {
    stack.add(VariableType.Value(onInputCallback()))
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
        value = stack.removeLastOrNull() as? VariableType.Type,
        lazyMessage = { "Type to coerce to was not provided" },
    ).type
    val value = requireNotNull(
        value = (stack.removeLastOrNull() as? VariableType.Value)?.value,
        lazyMessage = { "Value to coerce was not provided" },
    )
    stack.add(VariableType.Value(value.asType(type)))
}

internal fun Runtime.executeTypeExpression(instruction: Instruction.ExecuteTypeExpression) {
    stack.add(VariableType.Type(resolveTypeExpression(instruction.expression)))
}

internal fun Runtime.resolveTypeExpression(expression: TypeExpression): Type = when (expression) {
    is ArrayTypeExpression -> ArrayType(resolveTypeExpression(expression.typeExpression))
    is SetTypeExpression -> SetType(resolveTypeExpression(expression.typeExpression))
    is MapTypeExpression -> MapType(
        keyType = resolveTypeExpression(expression.keyTypeExpression),
        valueType = resolveTypeExpression(expression.valueTypeExpression),
    )

    is SymbolTypeExpression -> expression.symbolToken.toType().takeIf { it != UndefinedType }
        ?: requireNotNull(
            value = typeDeclarations[expression.symbolToken.value],
            lazyMessage = { "Type ${expression.symbolToken.value} was not found" },
        )
}

internal fun Runtime.executeExpression(instruction: Instruction.ExecuteExpression) {
    stack.add(
        VariableType.Value(
            when (instruction.expression) {
                is SymbolExpression -> requireNotNull(
                    value = variableScope.get(instruction.expression.token.value),
                    lazyMessage = { "Variable ${instruction.expression.token.value} was not found" },
                ).value

                is CharLiteralExpression -> instruction.expression.token.value
                is StringLiteralExpression -> instruction.expression.token.value
                is BooleanLiteralExpression -> instruction.expression.token.value.toBooleanStrict()
                is NumberLiteralExpression -> when (instruction.expression.token.type) {
                    Token.NumberLiteral.Type.Real, Token.NumberLiteral.Type.Exponent ->
                        instruction.expression.token.value.toRealNumber()

                    else -> instruction.expression.token.value.toIntNumber()
                }

                else -> throw IllegalStateException("Such expression should not be evaluated")
            },
        ),
    )
}
