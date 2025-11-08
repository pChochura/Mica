package com.pointlessapps.granite.mica.compiler.errors

internal class CompileTimeException(messageCallback: () -> String) : Exception(messageCallback())
