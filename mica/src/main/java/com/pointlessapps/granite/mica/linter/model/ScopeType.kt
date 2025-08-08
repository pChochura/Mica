package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.LoopIfStatement

internal sealed interface ScopeType {

    val allowFunctions: Boolean
    val allowVariables: Boolean

    data object Root : ScopeType {
        override val allowFunctions: Boolean = true
        override val allowVariables: Boolean = true
    }

    data class Function(val statement: FunctionDeclarationStatement) : ScopeType {
        override val allowFunctions: Boolean = false
        override val allowVariables: Boolean = true
    }

    data class If(val statement: IfConditionStatement) : ScopeType {
        override val allowFunctions: Boolean = false
        override val allowVariables: Boolean = true
    }

    data class LoopIf(val statement: LoopIfStatement) : ScopeType {
        override val allowFunctions: Boolean = false
        override val allowVariables: Boolean = true
    }
}
