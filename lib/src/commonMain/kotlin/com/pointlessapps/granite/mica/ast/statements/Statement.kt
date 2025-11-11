package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

/**
 * A sequence of tokens that execute an action.
 */
sealed class Statement(val startingToken: Token)
