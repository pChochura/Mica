package com.pointlessapps.granite.mica.linter.model

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement
import com.pointlessapps.granite.mica.ast.statements.LoopStatement
import com.pointlessapps.granite.mica.ast.statements.TypeDeclarationStatement

internal sealed interface ScopeType {

    val allowFunctions: Boolean
    val allowVariables: Boolean
    val allowTypes: Boolean

    data object Root : ScopeType {
        override val allowFunctions: Boolean = true
        override val allowVariables: Boolean = true
        override val allowTypes: Boolean = true
    }

    data class Type(val statement: TypeDeclarationStatement) : ScopeType {
        override val allowFunctions: Boolean = true
        override val allowVariables: Boolean = true
        override val allowTypes: Boolean = false
    }

    data class Function(val statement: FunctionDeclarationStatement) : ScopeType {
        override val allowFunctions: Boolean = false
        override val allowVariables: Boolean = true
        override val allowTypes: Boolean = false
    }

    data class If(val statement: IfConditionStatement) : ScopeType {
        override val allowFunctions: Boolean = false
        override val allowVariables: Boolean = true
        override val allowTypes: Boolean = false
    }

    data class Loop(val statement: LoopStatement) : ScopeType {
        override val allowFunctions: Boolean = false
        override val allowVariables: Boolean = true
        override val allowTypes: Boolean = false
    }
}
