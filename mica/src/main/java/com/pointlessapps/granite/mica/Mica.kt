package com.pointlessapps.granite.mica

import com.pointlessapps.granite.mica.compiler.Compiler
import com.pointlessapps.granite.mica.lexer.Lexer
import com.pointlessapps.granite.mica.linter.Linter
import com.pointlessapps.granite.mica.linter.model.Report
import com.pointlessapps.granite.mica.parser.Parser
import com.pointlessapps.granite.mica.vm.Interpreter

class Mica {
    suspend fun execute(
        input: String,
        onOutputCallback: (String) -> Unit,
        onInputCallback: suspend () -> String,
    ) {
        val rootAST = Parser(Lexer(input)).parse()
        val semanticAnalyzer = Linter(rootAST)
        val reports = semanticAnalyzer.analyze()

        reports.forEach { onOutputCallback(it.formatAsString()) }
        if (reports.any { it.type == Report.ReportType.ERROR }) {
            return
        }

        Interpreter(
            onOutputCallback = onOutputCallback,
            onInputCallback = onInputCallback,
        ).interpret(Compiler.compile(rootAST))
    }
}
