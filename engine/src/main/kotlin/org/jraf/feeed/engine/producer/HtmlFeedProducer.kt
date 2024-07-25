/*
 * This producer is part of the
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

package org.jraf.feeed.engine.producer

import org.jraf.feeed.api.feed.Feed
import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.producer.Producer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import us.codecraft.xsoup.Xsoup
import java.time.Instant

private val logger = LoggerFactory.getLogger(HtmlFeedProducer::class.java)

class HtmlFeedProducer(
  textProducer: Producer<String>,
  private val baseUrl: String,
  xPath: String,
) : PipedProducer<String, Feed>(upstreamProducer = textProducer) {

  private val xPathEvaluator = Xsoup.compile(xPath)

  override suspend fun produce(input: String): Result<Feed> {
    val document: Document = Jsoup.parse(input, baseUrl)
    val items = xPathEvaluator.evaluate(document).elements.map { aElement ->
      FeedItem(
        title = aElement.text(),
        link = aElement.attr("abs:href"),
        date = Instant.EPOCH,
        contents = "",
      )
    }
    return Result.success(Feed(items))
  }
}
