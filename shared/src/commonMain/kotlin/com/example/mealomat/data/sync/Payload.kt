package com.example.mealomat.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

fun payloadOf(vararg columns: Pair<String, JsonElement>): String =
    Json.encodeToString(JsonObject.serializer(), JsonObject(columns.toMap()))
