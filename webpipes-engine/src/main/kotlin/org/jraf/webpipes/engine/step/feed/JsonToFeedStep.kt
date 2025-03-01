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

package org.jraf.webpipes.engine.step.feed

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.jsonElementAtPath
import org.jraf.webpipes.engine.util.jsonObject
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import org.jraf.webpipes.engine.util.stringOrNull
import java.time.Instant

class JsonToFeedStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val jsonObject = context.jsonObject("json")
    val itemsPath = context.string("itemsPath").split(".")
    val itemsArray: JsonArray = jsonObject.jsonElementAtPath(itemsPath) as JsonArray

    val titleElementName = context.stringOrNull("titleFieldName")
    val linkElementName = context.stringOrNull("linkFieldName")
    val linkPrefix = context.stringOrNull("linkPrefix")
    val dateElementName = context.stringOrNull("dateFieldName")
    val bodyElementName = context.stringOrNull("bodyFieldName")
    val authorElementName = context.stringOrNull("authorFieldName")

    val feed: List<JsonObject> = itemsArray.mapNotNull { jsonElement ->
      val obj = jsonElement.jsonObject
      buildJsonObject {
        titleElementName?.let { titleFieldName ->
          put("title", obj.stringOrNull(titleFieldName))
        }
        linkElementName?.let { linkFieldName ->
          put(
            "link",
            if (linkPrefix != null) {
              obj.stringOrNull(linkFieldName)?.let { linkPrefix + it }
            } else {
              obj.stringOrNull(linkFieldName)
            },
          )
        }
        dateElementName?.let { dateFieldName ->
          put("date", obj.stringOrNull(dateFieldName)?.let { parseDate(it) })
        }
        bodyElementName?.let { bodyFieldName ->
          put("body", obj.stringOrNull(bodyFieldName))
        }
        authorElementName?.let { authorFieldName ->
          put("author", obj.stringOrNull(authorFieldName))
        }
      }
    }
    return context + ("feed" to JsonArray(feed))
  }

  private fun parseDate(text: String): String? {
    for (t in listOf(
      text,
      text + "Z",
      text.replace(" ", "T") + "Z",
    )) {
      runCatching { Instant.parse(t) }.getOrNull()?.let { return it.toString() }
    }
    return null
  }
}
