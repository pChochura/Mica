package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

internal class CommentStatement(val token: Token.Comment) : Statement(token)
