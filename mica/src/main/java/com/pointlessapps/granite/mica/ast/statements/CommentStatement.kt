package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token

/**
 * Statement that represents a comment. It is not part of the AST.
 *
 * Examples:
 *  - `// This is a comment`
 *  - `/* This is a multiline comment */`
 */
internal class CommentStatement(val token: Token.Comment) : Statement(token)
