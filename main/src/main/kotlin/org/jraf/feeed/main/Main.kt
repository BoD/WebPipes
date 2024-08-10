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
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.api.producer.value
import org.jraf.feeed.atom.FeedToAtom
import org.jraf.feeed.engine.producer.HtmlCropProducer
import org.jraf.feeed.engine.producer.HtmlFeedProducer
import org.jraf.feeed.engine.producer.UrlTextProducer
import org.jraf.feeed.engine.producer.generic.AddFeedItemFieldToContextProducer
import org.jraf.feeed.engine.producer.generic.AddToContextProducer
import org.jraf.feeed.engine.producer.generic.FeedItemMapFieldProducer
import org.jraf.feeed.engine.producer.generic.FeedItemMapProducer
import org.jraf.feeed.engine.producer.generic.pipe
import org.jraf.feeed.server.Server
import org.slf4j.LoggerFactory
import java.time.Instant

class Main {
  private val logger = LoggerFactory.getLogger(Main::class.java)

  fun start() {
    logger.info("Starting server")

    val url =
      "https://www.ugc.fr/filmsAjaxAction!getFilmsAndFilters.action?filter=stillOnDisplay&page=30010&cinemaId=&reset=false&__multiselect_versions=&labels=UGC%20Culte&__multiselect_labels=&__multiselect_groupeImages="

    val producer =
      UrlTextProducer() pipe
        AddToContextProducer(
          "baseUrl" to url,
          "xPath" to "//div[@class='info-wrapper']//a",
        ) pipe
        HtmlFeedProducer() pipe
        FeedItemMapProducer(
          AddToContextProducer<FeedItem>(
            "fieldIn" to FeedItem.Field.Link,
            "fieldOut" to FeedItem.Field.Body,
          ) pipe
            AddFeedItemFieldToContextProducer(
              FeedItem.Field.Link,
              "baseUrl",
            ) pipe
            FeedItemMapFieldProducer(
              UrlTextProducer() pipe
                AddToContextProducer(
                  "xPath" to "//div[@class='component--film-presentation d-flex flex-wrap']",
                ) pipe
                HtmlCropProducer()
            )
        )

    val feedToAtom = FeedToAtom()

    Server { requestParams ->
      val feed = producer.produce(ProducerContext(), url).getOrThrow().value
      feedToAtom.convert(
        source = feed,
        atomTitle = "UGC Culte",
        atomDescription = "UGC Culte",
        atomLink = requestParams.requestUrl,
        atomPublishedDate = Instant.now(),
      )
    }.start()
  }
}

fun main() {
  Main().start()
}
