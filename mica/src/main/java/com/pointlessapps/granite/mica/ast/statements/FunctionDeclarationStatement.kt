package com.pointlessapps.granite.mica.ast.statements

import com.pointlessapps.granite.mica.model.Token
import com.pointlessapps.granite.mica.semantics.mapper.toType

/**
 * Statement that declares a function.
 *
 * Examples:
 *  - `add(a: number, b: number): number { return 1 }`
 *  - `method() {}`
 *  - `method2() { return }`
 */
internal class FunctionDeclarationStatement(
    val nameToken: Token.Symbol,
    val openBracketToken: Token.BracketOpen,
    val closeBracketToken: Token.BracketClose,
    val openCurlyToken: Token.CurlyBracketOpen,
    val closeCurlyToken: Token.CurlyBracketClose,
    val colonToken: Token.Colon?,
    val returnTypeToken: Token.Symbol?,
    val parameters: List<FunctionParameterDeclarationStatement>,
    val body: List<Statement>,
) : Statement(nameToken) {

    val parameterTypes = parameters.associateWith { it.typeToken.toType() }
    val returnType = returnTypeToken?.toType()

    /**
     * Function signature in a format:
     * <function name>(<parameter type>,<parameter type>,...)
     */
    val signature = "${nameToken.value}(${
        parameterTypes.map { it.value?.name ?: it.key.typeToken.value }.joinToString()
    })"
}

internal class FunctionParameterDeclarationStatement(
    val nameToken: Token.Symbol,
    val colonToken: Token.Colon,
    val typeToken: Token.Symbol,
)
