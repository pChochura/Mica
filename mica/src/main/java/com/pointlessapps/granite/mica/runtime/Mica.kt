package com.pointlessapps.granite.mica.runtime

import com.pointlessapps.granite.mica.lexer.Lexer
import com.pointlessapps.granite.mica.linter.Linter
import com.pointlessapps.granite.mica.linter.model.Report
import com.pointlessapps.granite.mica.parser.Parser

class Mica {
    fun execute(input: String) {
        val rootAST = Parser(Lexer(input)).parse()
        val semanticAnalyzer = Linter(rootAST)
        val reports = semanticAnalyzer.analyze()

        reports.forEach { onOutputCallback(it.formatAsString()) }
        if (reports.any { it.type == Report.ReportType.ERROR }) {
            return
        }

        Runtime(rootAST).execute()
    }
}
