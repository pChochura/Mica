package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.ast.expressions.Expression
import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.model.Token

internal sealed class Instruction {
    data class Label(val label: String) : Instruction()
    data class Jump(val label: String, var index: Int = -1) : Instruction()
    data class JumpIf(val condition: Boolean, val label: String, var index: Int = -1) : Instruction()
    data object ReturnFromFunction : Instruction()

    data class DeclareVariable(val variableName: String) : Instruction()
    data class AssignVariable(val variableName: String) : Instruction()

    data class ExecuteTypeExpression(val expression: TypeExpression) : Instruction()
    data class ExecuteExpression(val expression: Expression) : Instruction()
    data class ExecuteBinaryOperation(val operator: Token.Operator.Type) : Instruction()
    data class ExecuteUnaryOperation(val operator: Token.Operator.Type) : Instruction()
    data class ExecuteArrayLiteralExpression(val elementsCount: Int) : Instruction()
    data object ExecuteArrayIndexExpression : Instruction()
    data class ExecuteFunctionCallExpression(val argumentsCount: Int) : Instruction()

    data object DeclareScope : Instruction()
    data object ExitScope : Instruction()
    data object DuplicateLastStackItem : Instruction()

    data object AcceptInput : Instruction()
    data object Print : Instruction()
}
