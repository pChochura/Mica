package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that represents a line that contains whitespaces.
 */
internal data class EmptyStatement(val token: Token) : Statement(token)
