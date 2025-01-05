/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2024-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jraf.webpipes.engine.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val JsonElement.stringOrNull: String? get() = jsonPrimitive.contentOrNull
val JsonElement.string: String get() = stringOrNull ?: error("Not a string")

fun JsonObject.stringOrNull(key: String): String? = get(key)?.jsonPrimitive?.contentOrNull
fun JsonObject.string(key: String): String = stringOrNull(key) ?: error("Missing key '$key'")
fun JsonObject.string(key: String, defaultValue: String): String = stringOrNull(key) ?: defaultValue

fun JsonObject.booleanOrNull(key: String): Boolean? = get(key)?.jsonPrimitive?.booleanOrNull
fun JsonObject.boolean(key: String): Boolean = booleanOrNull(key) ?: error("Missing key '$key'")
fun JsonObject.boolean(key: String, defaultValue: Boolean): Boolean = booleanOrNull(key) ?: defaultValue

fun JsonObject.intOrNull(key: String): Int? = get(key)?.jsonPrimitive?.intOrNull
fun JsonObject.int(key: String): Int = intOrNull(key) ?: error("Missing key '$key'")
fun JsonObject.int(key: String, defaultValue: Int): Int = intOrNull(key) ?: defaultValue

fun JsonObject.jsonObjectOrNull(key: String): JsonObject? = get(key)?.jsonObject
fun JsonObject.jsonObject(key: String): JsonObject = jsonObjectOrNull(key) ?: error("Missing key '$key'")
fun JsonObject.jsonObject(key: String, defaultValue: JsonObject): JsonObject = jsonObjectOrNull(key) ?: defaultValue

fun JsonObject.jsonArrayOrNull(key: String): JsonArray? = get(key) as? JsonArray
fun JsonObject.jsonArray(key: String): JsonArray = jsonArrayOrNull(key) ?: error("Missing key '$key'")
fun JsonObject.jsonArray(key: String, defaultValue: JsonArray): JsonArray = jsonArrayOrNull(key) ?: defaultValue

inline operator fun <reified T : Any> JsonObject.plus(element: Pair<String, T?>): JsonObject =
  JsonObject(
    (this as Map<String, JsonElement>) + (element.first to (element.second?.let {
      when (it) {
        is String -> JsonPrimitive(it)
        is Boolean -> JsonPrimitive(it)
        is Number -> JsonPrimitive(it)
        is JsonElement -> it
        else -> error("Unsupported type: ${it::class}")
      }
    } ?: JsonNull)),
  )

operator fun JsonObject.plus(other: JsonObject): JsonObject =
  JsonObject(
    (this as Map<String, JsonElement>) + (other as Map<String, JsonElement>),
  )
