package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token

internal sealed class Instruction {
    data class Label(val label: String) : Instruction()
    data class Jump(val label: String, var index: Int = -1) : Instruction()
    data class JumpIf(
        val condition: Boolean,
        val label: String,
        var index: Int = -1,
    ) : Instruction()

    data object ReturnFromFunction : Instruction()

    data object DeclareCustomObjectProperties : Instruction()
    data class CreateCustomObject(val typeName: String, val propertyNames: List<String>) :
        Instruction()

    data class DeclareType(val typeName: String) : Instruction()
    data class DeclareFunction(val functionName: String, val parametersCount: Int) : Instruction()
    data class DeclareVariable(val variableName: String) : Instruction()
    data class AssignVariable(val variableName: String) : Instruction()

    data class DuplicateLastStackItems(val count: Int) : Instruction()
    data class PushToStack(val value: Variable<*>) : Instruction()
    data object SaveFromStack : Instruction()
    data object RestoreToStack : Instruction()

    data object ExecuteTypeCoercionExpression : Instruction()
    data class ExecuteTypeExpression(val expression: TypeExpression) : Instruction()
    data class ExecuteExpression(val expression: Expression) : Instruction()
    data class ExecuteBinaryOperation(val operator: Token.Operator.Type) : Instruction()
    data class ExecuteUnaryOperation(val operator: Token.Operator.Type) : Instruction()
    data class ExecuteArrayLiteralExpression(val elementsCount: Int) : Instruction()
    data class ExecuteSetLiteralExpression(val elementsCount: Int) : Instruction()
    data class ExecuteArrayIndexGetExpression(val depth: Int) : Instruction()
    data class ExecuteArrayIndexSetExpression(val depth: Int) : Instruction()
    data object ExecuteArrayLengthExpression : Instruction()
    data class ExecuteFunctionCallExpression(
        val functionName: String,
        val argumentsCount: Int,
    ) : Instruction()

    data class ExecuteCustomObjectPropertyAccessExpression(val propertyName: String) : Instruction()

    data object DeclareScope : Instruction()
    data object ExitScope : Instruction()

    data object AcceptInput : Instruction()
    data object Print : Instruction()
}
