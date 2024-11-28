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

package org.jraf.feeed.main.bsky

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jraf.feeed.api.feed.Feed
import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.engine.producer.core.pipe
import java.time.Instant

class BlueSkyJsonToFeedProducer : Producer<List<JsonElement>, Feed> {
  override suspend fun produce(context: ProducerContext, input: List<JsonElement>): Result<Pair<ProducerContext, Feed>> {
    return runCatching {
      val items = input.mapNotNull { jsonElement ->
        val obj = jsonElement.jsonObject
        val reply = obj["reply"]
        if (reply != null) {
          return@mapNotNull null
        }
        val post = obj["post"]!!.jsonObject
        val record = post["record"]!!.jsonObject
        val author = post["author"]!!
        val authorHandle = author.jsonObject["handle"]!!.string
        val authorDisplayName = author.jsonObject["displayName"]!!.string
        val postId = post["uri"]!!.string.substringAfterLast('/')
        val link = "https://bsky.app/profile/${authorHandle}/post/${postId}"
        FeedItem(
          title = "$authorDisplayName - $link",
          link = link,
          date = Instant.parse(record["createdAt"]!!.string),
          body = null,
          extras = mapOf(
            "author" to authorHandle,
          ),
        )
      }
      context to Feed(items)
    }
  }

  override fun close() {}
}

fun <IN> Producer<IN, List<JsonElement>>.blueSkyJsonToFeed(): Producer<IN, Feed> {
  return pipe(BlueSkyJsonToFeedProducer())
}

private val JsonElement.string: String
  get() = jsonPrimitive.content
