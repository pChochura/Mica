package com.pointlessapps.granite.mica.parser.statement

import com.pointlessapps.granite.mica.ast.statements.FunctionDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.TypeDeclarationStatement
import com.pointlessapps.granite.mica.ast.statements.TypePropertyDeclarationStatement
import com.pointlessapps.granite.mica.model.Keyword
import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.parser.expression.parseTypeExpression
import com.pointlessapps.granite.mica.parser.isPropertyDeclarationStatementStarting

internal fun Parser.parseTypeDeclarationStatement(): TypeDeclarationStatement {
    val typeToken = expectToken<Token.Keyword>("type definition statement") {
        it.value == Keyword.TYPE.value
    }
    val nameToken = expectToken<Token.Symbol>("type definition statement") { it !is Token.Keyword }
    // TODO make those optional
    val colonToken = expectToken<Token.Colon>("type definition statement")
    val baseTypeExpression = parseTypeExpression { it is Token.EOL || it is Token.CurlyBracketOpen }

    val openCurlyToken = expectToken<Token.CurlyBracketOpen>("type definition statement")

    skipTokens<Token.EOL>()
    val properties = parseTypePropertyDeclarationStatements()

    val functions = mutableListOf<FunctionDeclarationStatement>()
    while (!isToken<Token.CurlyBracketClose>()) {
        skipTokens<Token.EOL>()
        functions.add(parseFunctionDeclarationStatement())
        skipTokens<Token.EOL>()
    }
    val closeCurlyToken = expectToken<Token.CurlyBracketClose>("type definition statement")

    expectEOForEOL("type definition statement")

    return TypeDeclarationStatement(
        typeToken = typeToken,
        nameToken = nameToken,
        colonToken = colonToken,
        baseTypeExpression = baseTypeExpression,
        openCurlyToken = openCurlyToken,
        properties = properties,
        functions = functions,
        closeCurlyToken = closeCurlyToken,
    )
}

internal fun Parser.parseTypePropertyDeclarationStatements(): List<TypePropertyDeclarationStatement> {
    val properties = mutableListOf<TypePropertyDeclarationStatement>()
    while (isToken<Token.Symbol>()) {
        if (!isPropertyDeclarationStatementStarting()) break

        val propertyNameToken = expectToken<Token.Symbol>("type property declaration") {
            it !is Token.Keyword
        }
        val propertyColonToken = expectToken<Token.Colon>("type property declaration")
        val propertyTypeExpression = parseTypeExpression { it is Token.EOL }

        properties.add(
            TypePropertyDeclarationStatement(
                nameToken = propertyNameToken,
                colonToken = propertyColonToken,
                typeExpression = propertyTypeExpression,
            ),
        )

        skipTokens<Token.EOL>()
    }

    return properties.toList()
}
