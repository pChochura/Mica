package com.pointlessapps.granite.mica.errors

import com.pointlessapps.granite.mica.model.Location

open class LocationReportingException(location: Location, message: String) :
    Exception("${location.line}:${location.column}: ERROR: $message")
