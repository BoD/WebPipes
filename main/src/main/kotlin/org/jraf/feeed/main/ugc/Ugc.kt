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

package org.jraf.feeed.main.ugc

import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.api.producer.ProducerContextReference
import org.jraf.feeed.api.producer.context
import org.jraf.feeed.api.producer.value
import org.jraf.feeed.atom.atom
import org.jraf.feeed.engine.producer.core.IdentityProducer
import org.jraf.feeed.engine.producer.core.addInputToContext
import org.jraf.feeed.engine.producer.core.addToContext
import org.jraf.feeed.engine.producer.core.cache
import org.jraf.feeed.engine.producer.feed.AddFeedItemFieldToContextProducer
import org.jraf.feeed.engine.producer.feed.feedFilter
import org.jraf.feeed.engine.producer.feed.feedItemMap
import org.jraf.feeed.engine.producer.feed.feedItemMapField
import org.jraf.feeed.engine.producer.feed.feedItemTextContains
import org.jraf.feeed.engine.producer.feed.feedMaxItems
import org.jraf.feeed.engine.producer.feed.mergeFeeds
import org.jraf.feeed.engine.producer.html.htmlCrop
import org.jraf.feeed.engine.producer.html.htmlFeed
import org.jraf.feeed.engine.producer.net.UrlTextProducer
import org.jraf.feeed.engine.producer.net.urlText
import org.jraf.feeed.server.RequestParams

private val url =
  "https://www.ugc.fr/filmsAjaxAction!getFilmsAndFilters.action?filter=stillOnDisplay&page=30010&cinemaId=&reset=false&__multiselect_versions=&labels=UGC%20Culte&__multiselect_labels=&__multiselect_groupeImages="

private val producer: Producer<String, String> =
  IdentityProducer<String>()
    .addInputToContext(key = "baseUrl")
    .urlText()
    .htmlFeed(aElementsXPath = "//div[@class='info-wrapper']//a")
    .feedItemMap(
      AddFeedItemFieldToContextProducer(FeedItem.Field.Link, "baseUrl")
        .feedItemMapField(
          mapper = UrlTextProducer(),
          fieldIn = FeedItem.Field.Link,
          fieldOut = FeedItem.Field.Body,
        ),
    )
    .feedItemMap(
      IdentityProducer<FeedItem>()
        .feedItemTextContains(
          fieldIn = FeedItem.Field.Body,
          textToFind = "lyon",
          fieldOut = FeedItem.Field.Extra("isLyon"),
        ),
    )
    .feedFilter(FeedItem.Field.Extra("isLyon"))
    .feedItemMap(
      IdentityProducer<FeedItem>()
        .feedItemMapField(
          mapper = IdentityProducer<String>()
            .addToContext(
              "xPath" to "//div[@class='group-info d-none d-md-block'][4]/p[2]",
              "extractText" to true,
            )
            .htmlCrop(),
          fieldIn = FeedItem.Field.Body,
          fieldOut = FeedItem.Field.Body,
        ),
    )
    .mergeFeeds()
    .feedMaxItems()
    .addInputToContext(key = "feed")
    .addToContext("atomLink" to ProducerContextReference("requestUrl"))
    .atom(
      atomTitle = "UGC Culte",
      atomDescription = "UGC Culte",
      atomEntriesAuthor = "UGC",
    )
    .cache()

private var context = ProducerContext()

suspend fun produceUgc(requestParams: RequestParams): String {
  val output = producer
    .produce(
      context.with("requestUrl", requestParams.requestUrl),
      url,
    )
    .getOrThrow()
  context = output.context
  return output.value
}
