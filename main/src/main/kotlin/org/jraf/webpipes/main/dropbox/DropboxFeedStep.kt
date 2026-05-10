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

package org.jraf.webpipes.main.dropbox

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import java.time.Instant

class DropboxFeedStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val latestFileUrlJsonStr: String = context.string("text")
    val url = Json.parseToJsonElement(latestFileUrlJsonStr).jsonObject.string("url")
    val feed: List<JsonObject> = listOf(
      buildJsonObject {
        put("link", url)
        @OptIn(ExperimentalSerializationApi::class)
        put("body", "<html><body><img src=\"$url\"></body></html>")
        put("date", Instant.now().toString())
      },
    )
    return context + ("feed" to JsonArray(feed))
  }
}
