package com.pointlessapps.granite.mica.compiler.errors

internal class RunTimeException(messageCallback: () -> String) : Exception(messageCallback())
