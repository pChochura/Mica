package com.pointlessapps.granite.mica.semantics.model

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.IfConditionStatement

internal sealed interface ScopeType {
    data object Root : ScopeType
    data class Function(val functionDeclarationStatement: FunctionDeclarationStatement) : ScopeType
    data class If(val ifConditionStatement: IfConditionStatement) : ScopeType
}
