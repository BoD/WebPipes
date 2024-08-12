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

package org.jraf.feeed.main

import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.api.producer.ProducerContextReference
import org.jraf.feeed.api.producer.context
import org.jraf.feeed.api.producer.value
import org.jraf.feeed.atom.atom
import org.jraf.feeed.engine.producer.UrlTextProducer
import org.jraf.feeed.engine.producer.core.IdentityProducer
import org.jraf.feeed.engine.producer.core.addInputToContext
import org.jraf.feeed.engine.producer.core.addToContext
import org.jraf.feeed.engine.producer.core.cache
import org.jraf.feeed.engine.producer.feed.AddFeedItemFieldToContextProducer
import org.jraf.feeed.engine.producer.feed.feedItemMap
import org.jraf.feeed.engine.producer.feed.feedItemMapField
import org.jraf.feeed.engine.producer.feed.feedMaxItems
import org.jraf.feeed.engine.producer.feed.mergeFeeds
import org.jraf.feeed.engine.producer.html.htmlCrop
import org.jraf.feeed.engine.producer.html.htmlFeed
import org.jraf.feeed.engine.producer.urlText
import org.jraf.feeed.server.Server
import org.slf4j.LoggerFactory

class Main {
  private val logger = LoggerFactory.getLogger(Main::class.java)

  fun start() {
    logger.info("Starting server")

    val url =
      "https://www.ugc.fr/filmsAjaxAction!getFilmsAndFilters.action?filter=stillOnDisplay&page=30010&cinemaId=&reset=false&__multiselect_versions=&labels=UGC%20Culte&__multiselect_labels=&__multiselect_groupeImages="

    val producer: Producer<String, String> =
      IdentityProducer<String>()
        .addToContext("key" to "baseUrl")
        .addInputToContext()
        .urlText()
        .addToContext("AElementsXPath" to "//div[@class='info-wrapper']//a")
        .htmlFeed()
        .feedItemMap(
          AddFeedItemFieldToContextProducer(FeedItem.Field.Link, "baseUrl")
            .addToContext(
              "fieldIn" to FeedItem.Field.Link,
              "fieldOut" to FeedItem.Field.Body,
            )
            .feedItemMapField(
              UrlTextProducer()
                .addToContext(
                  "xPath" to "//div[@class='group-info d-none d-md-block'][4]/p[2]",
                  "extractText" to true,
                )
                .htmlCrop()
            )
        )
        .mergeFeeds()
        .feedMaxItems()
        .addToContext("key" to "feed")
        .addInputToContext()
        .addToContext(
          "atomTitle" to "UGC Culte",
          "atomDescription" to "UGC Culte",
          "atomLink" to ProducerContextReference("requestUrl"),
          "atomEntriesAuthor" to "UGC",
        )
        .atom()
        .cache()

    var context = ProducerContext()

    Server { requestParams ->
      val output = producer
        .produce(
          context.with("requestUrl", requestParams.requestUrl),
          url
        )
        .getOrThrow()
      context = output.context
      output.value
    }.start()
  }
}

fun main() {
  Main().start()
}
