package com.pointlessapps.granite.mica.runtime.model

import com.pointlessapps.granite.mica.ast.expressions.Expression

internal sealed class Instruction {
    data class Label(val label: String) : Instruction()
    data class Jump(val label: String, var index: Int = -1) : Instruction()
    data class JumpIfFalse(val label: String, var index: Int = -1) : Instruction()
    data object ReturnFromFunction : Instruction()

    data class DeclareVariable(val variableName: String) : Instruction()
    data class AssignVariable(val variableName: String) : Instruction()
    data class ExecuteExpression(val expression: Expression) : Instruction()

    data object AcceptInput : Instruction()
    data object Print : Instruction()
}
