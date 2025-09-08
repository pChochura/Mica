package com.pointlessapps.granite.mica.builtins

import com.pointlessapps.granite.mica.builtins.functions.anyBuiltinFunctions
import com.pointlessapps.granite.mica.builtins.functions.arrayBuiltinFunctions
import com.pointlessapps.granite.mica.builtins.functions.customObjectBuiltinFunctions
import com.pointlessapps.granite.mica.builtins.functions.mapBuiltinFunctions
import com.pointlessapps.granite.mica.builtins.functions.rangeBuiltinFunctions
import com.pointlessapps.granite.mica.builtins.functions.setBuiltinFunctions
import com.pointlessapps.granite.mica.builtins.functions.stringBuiltinFunctions
import com.pointlessapps.granite.mica.builtins.functions.typeBuiltinFunctions
import com.pointlessapps.granite.mica.builtins.functions.typeConversionBuiltinFunctions
import com.pointlessapps.granite.mica.builtins.properties.arrayBuiltinTypeProperties
import com.pointlessapps.granite.mica.builtins.properties.rangeBuiltinTypeProperties

internal val builtinFunctions = anyBuiltinFunctions +
        typeConversionBuiltinFunctions +
        arrayBuiltinFunctions +
        mapBuiltinFunctions +
        customObjectBuiltinFunctions +
        setBuiltinFunctions +
        typeBuiltinFunctions +
        stringBuiltinFunctions +
        rangeBuiltinFunctions

internal val builtinTypeProperties = rangeBuiltinTypeProperties + arrayBuiltinTypeProperties
