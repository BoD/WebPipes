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

@file:OptIn(ExperimentalSerializationApi::class)

package org.jraf.feeed.main.bsky

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.jraf.feeed.api.Step
import org.jraf.feeed.engine.util.jsonArray
import org.jraf.feeed.engine.util.jsonObject
import org.jraf.feeed.engine.util.plus
import org.jraf.feeed.engine.util.string
import java.time.Instant

class BlueSkyJsonToFeedStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val bskyFeedObject = context.jsonObject("json")
    val bskyFeedArray: List<JsonElement> = bskyFeedObject.jsonArray("feed")
    val feed: List<JsonObject> = bskyFeedArray.mapNotNull { jsonElement ->
      val obj = jsonElement.jsonObject
      val reply = obj["reply"]
      if (reply != null) {
        return@mapNotNull null
      }
      val post = obj.jsonObject("post")
      val record = post.jsonObject("record")
      val author = post.jsonObject("author")
      val authorHandle = author.string("handle")
      val authorDisplayName = author.string("displayName")
      val postId = post.string("uri").substringAfterLast('/')
      val link = "https://bsky.app/profile/${authorHandle}/post/${postId}"
      buildJsonObject {
        put("title", "$authorDisplayName - $link")
        put("link", link)
        put("date", Instant.parse(record["createdAt"]!!.string).toString())
        put("body", null)
        put("author", authorHandle)
      }
    }
    return context + ("feed" to JsonArray(feed))
  }
}
