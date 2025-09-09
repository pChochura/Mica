package com.pointlessapps.granite.mica.errors

import com.pointlessapps.granite.mica.model.Token

class UnexpectedTokenException(
    expectedToken: String,
    actualToken: Token,
    currentlyParsing: String,
) : LocationReportingException(
    location = actualToken.location,
    message = "Expected $expectedToken, but got $actualToken, while parsing $currentlyParsing",
)
