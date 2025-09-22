package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.expressions.TypeExpression
import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.TypeDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.TypePropertyDeclaration
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseTypeExpression

internal fun Parser.parseTypeDeclarationStatement(): TypeDeclarationStatement {
    val typeToken = expectToken<Token.Keyword>("type declaration statement") {
        it.value == Keyword.TYPE.value
    }
    val nameToken = expectToken<Token.Symbol>("type declaration statement") { it !is Token.Keyword }

    var atToken: Token.At? = null
    var typeParameterConstraint: TypeExpression? = null
    if (isToken<Token.At>()) {
        atToken = expectToken<Token.At>("type declaration statement - type parameter constraint")
        typeParameterConstraint = parseTypeExpression {
            it is Token.Colon || it is Token.CurlyBracketOpen || it is Token.EOL
        }
    }

    var colonToken: Token.Colon? = null
    var typeExpression: TypeExpression? = null
    if (isToken<Token.Colon>()) {
        colonToken = expectToken<Token.Colon>("type declaration statement - parent type")
        typeExpression = parseTypeExpression { it is Token.CurlyBracketOpen || it is Token.EOL }
    }

    val openCurlyToken = expectToken<Token.CurlyBracketOpen>("type declaration statement")

    skipTokens<Token.EOL>()
    val properties = parseTypePropertyDeclarationStatements()

    val functions = mutableListOf<FunctionDeclarationStatement>()
    while (!isToken<Token.CurlyBracketClose>()) {
        skipTokens<Token.EOL>()
        functions.add(parseFunctionDeclarationStatement())
        skipTokens<Token.EOL>()
    }
    val closeCurlyToken = expectToken<Token.CurlyBracketClose>("type declaration statement")

    expectEOForEOL("type declaration statement")

    return TypeDeclarationStatement(
        typeToken = typeToken,
        nameToken = nameToken,
        atToken = atToken,
        typeParameterConstraint = typeParameterConstraint,
        colonToken = colonToken,
        parentTypeExpression = typeExpression,
        openCurlyToken = openCurlyToken,
        properties = properties,
        functions = functions,
        closeCurlyToken = closeCurlyToken,
    )
}

private fun Parser.parseTypePropertyDeclarationStatements(): List<TypePropertyDeclaration> {
    val properties = mutableListOf<TypePropertyDeclaration>()
    while (isToken<Token.Symbol>()) {
        val savedIndex = currentIndex
        val propertyNameToken = expectToken<Token.Symbol>("type property declaration") {
            it !is Token.Keyword
        }

        if (!isToken<Token.Colon>()) {
            restoreTo(savedIndex)
            break
        }

        val propertyColonToken = expectToken<Token.Colon>("type property declaration")
        val propertyTypeExpression = parseTypeExpression { it is Token.EOL }

        properties.add(
            TypePropertyDeclaration(
                nameToken = propertyNameToken,
                colonToken = propertyColonToken,
                typeExpression = propertyTypeExpression,
            ),
        )

        skipTokens<Token.EOL>()
    }

    return properties.toList()
}
