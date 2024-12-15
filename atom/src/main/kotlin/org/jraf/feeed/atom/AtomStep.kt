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

package org.jraf.feeed.atom

import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedOutput
import kotlinx.serialization.json.JsonObject
import org.jraf.feeed.api.Step
import org.jraf.feeed.engine.util.jsonArray
import org.jraf.feeed.engine.util.plus
import org.jraf.feeed.engine.util.string
import org.jraf.feeed.engine.util.stringOrNull
import java.time.Instant
import java.util.Date

class AtomStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val feed = context.jsonArray("feed")
    val atomTitle = context.string("atomTitle")
    val atomDescription = context.string("atomDescription")
    val atomLink = context.string("atomLink")
    val atomPublishedDate: Instant? = context.stringOrNull("atomPublishedDate")?.let { Instant.parse(it) }

    val syndFeed = SyndFeedImpl()
    syndFeed.feedType = "atom_1.0"
    syndFeed.title = atomTitle
    syndFeed.description = atomDescription
    syndFeed.link = atomLink
    syndFeed.uri = atomLink
    syndFeed.publishedDate = atomPublishedDate?.let { Date.from(it) }
      ?: Date.from(feed.maxOf { feedItem -> Instant.parse((feedItem as JsonObject).string("date")) })
    syndFeed.entries = feed.map { feedItem ->
      feedItem as JsonObject
      SyndEntryImpl().apply {
        title = feedItem.string("title")
        link = feedItem.string("link")
        uri = feedItem.string("link")
        feedItem.stringOrNull("body")?.let { body ->
          contents = listOf(
            SyndContentImpl().apply {
              type = "text/html"
              value = body
            },
          )
        }
        publishedDate = Date.from(Instant.parse(feedItem.string("date")))
        author = feedItem.stringOrNull("author") ?: context.stringOrNull("atomEntriesAuthor")
      }
    }
    return context + ("text" to SyndFeedOutput().outputString(syndFeed))
  }
}
