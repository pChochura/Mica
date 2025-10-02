package com.pointlessapps.granite.mica.compiler.helper

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal val uniqueId: String
    get() = Uuid.random().toHexString()
