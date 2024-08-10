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

package org.jraf.feeed.engine.producer.html

import org.jraf.feeed.api.feed.Feed
import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.engine.producer.generic.pipe
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import us.codecraft.xsoup.Xsoup
import java.time.Instant

class HtmlFeedProducer : Producer<String, Feed> {
  override suspend fun produce(context: ProducerContext, input: String): Result<Pair<ProducerContext, Feed>> {
    return runCatching {
      val baseUrl: String = context["baseUrl"]
      val xPath: String = context["xPath"]
      val xPathEvaluator = Xsoup.compile(xPath)

      val document: Document = Jsoup.parse(input, baseUrl)
      val items = xPathEvaluator.evaluate(document).elements.map { aElement ->
        FeedItem(
          title = aElement.text(),
          link = aElement.absUrl("href"),
          date = Instant.now(),
          body = "",
        )
      }
      context to Feed(items)
    }
  }

  override fun close() {}
}

fun <IN> Producer<IN, String>.htmlFeed(): Producer<IN, Feed> {
  return pipe(HtmlFeedProducer())
}
