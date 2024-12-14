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
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedOutput
import org.jraf.feeed.api.feed.Feed
import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.step.Context
import org.jraf.feeed.api.step.Step
import org.jraf.feeed.engine.producer.core.StepChain
import org.jraf.feeed.engine.producer.core.addToContextIfNotNull
import java.time.Instant
import java.util.Date

class AtomStep : Step {
  override suspend fun execute(context: Context): Result<Context> {
    return runCatching {
      val feed: Feed = context["feed"]
      val atomTitle: String = context["atomTitle"]
      val atomDescription: String = context["atomDescription"]
      val atomLink: String = context["atomLink"]
      val atomPublishedDate: Instant? = context["atomPublishedDate", null]

      val syndFeed: SyndFeed = SyndFeedImpl()
      syndFeed.feedType = "atom_1.0"
      syndFeed.title = atomTitle
      syndFeed.description = atomDescription
      syndFeed.link = atomLink
      syndFeed.uri = atomLink
      syndFeed.publishedDate = atomPublishedDate?.let { Date.from(it) } ?: Date.from(feed.items.maxOf { it.date })
      syndFeed.entries = feed.items.map { feedItem ->
        SyndEntryImpl().apply {
          title = feedItem.title
          link = feedItem.link
          uri = feedItem.link
          feedItem.body?.let { body ->
            contents = listOf(
              SyndContentImpl().apply {
                type = "text/html"
                value = body
              },
            )
          }
          publishedDate = Date.from(feedItem.date)
          author = feedItem[FeedItem.Field.Extra("author")] ?: context["atomEntriesAuthor", null]
        }
      }
      context.with("text", SyndFeedOutput().outputString(syndFeed))
    }
  }
}

fun StepChain.atom(
  atomTitle: String? = null,
  atomDescription: String? = null,
  atomLink: String? = null,
  atomPublishedDate: Instant? = null,
  atomEntriesAuthor: String? = null,
): StepChain {
  return addToContextIfNotNull("atomTitle", atomTitle)
    .addToContextIfNotNull("atomDescription", atomDescription)
    .addToContextIfNotNull("atomLink", atomLink)
    .addToContextIfNotNull("atomPublishedDate", atomPublishedDate)
    .addToContextIfNotNull("atomEntriesAuthor", atomEntriesAuthor) +
    AtomStep()
}
