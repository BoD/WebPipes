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

import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.api.producer.ProducerContextReference
import org.jraf.feeed.api.producer.context
import org.jraf.feeed.api.producer.value
import org.jraf.feeed.atom.atom
import org.jraf.feeed.engine.producer.core.IdentityProducer
import org.jraf.feeed.engine.producer.core.addInputToContext
import org.jraf.feeed.engine.producer.core.addToContext
import org.jraf.feeed.engine.producer.core.associate
import org.jraf.feeed.engine.producer.core.cache
import org.jraf.feeed.engine.producer.core.pipe
import org.jraf.feeed.engine.producer.core.removeFromContext
import org.jraf.feeed.engine.producer.core.setResult
import org.jraf.feeed.engine.producer.feed.feedMaxItems
import org.jraf.feeed.engine.producer.feed.mergeFeeds
import org.jraf.feeed.engine.producer.json.asArray
import org.jraf.feeed.engine.producer.json.asPrimitive
import org.jraf.feeed.engine.producer.json.extract
import org.jraf.feeed.engine.producer.json.json
import org.jraf.feeed.engine.producer.net.urlText
import org.jraf.feeed.engine.producer.text.prefix
import org.jraf.feeed.server.RequestParams

private val createSessionUrl = "https://bsky.social/xrpc/com.atproto.server.createSession"
private val getListFeedUrl =
  "https://bsky.social/xrpc/app.bsky.feed.getListFeed?list=at%3A%2F%2Fdid%3Aplc%3Azrwjh3urruteuvjonaajoq3r%2Fapp.bsky.graph.list%2F3lbclce6ypy2p&limit=30"

private val producer: Producer<String, String> =
  IdentityProducer<String>()
    .addInputToContext(key = "baseUrl")
    .addToContext("headers" to mapOf("Content-Type" to "application/json"))
    .pipe(
      object : Producer<String, String> {
        override suspend fun produce(context: ProducerContext, input: String): Result<Pair<ProducerContext, String>> {
          return Result.success(
            context.with("body", """{"identifier": "${context["identifier", ""]}", "password": "${context["password", ""]}"}""") to input,
          )
        }

        override fun close() {}
      },
    )
    .urlText()
    .json()
    .extract("accessJwt")
    .asPrimitive()
    .prefix("Bearer ")
    .associate("Authorization")
    .addInputToContext(key = "headers")
    .setResult(getListFeedUrl)
    .removeFromContext("body")
    .urlText()
    .json()
    .extract("feed")
    .asArray()
    .blueSkyJsonToFeed()
    .mergeFeeds()
    .feedMaxItems()
    .addInputToContext(key = "feed")
    .addToContext("atomLink" to ProducerContextReference("requestUrl"))
    .atom(
      atomTitle = "Bluesky",
      atomDescription = "Bluesky",
      atomEntriesAuthor = "Feeed",
    )
    .cache()

private var context = ProducerContext()

suspend fun produceBlueSky(requestParams: RequestParams): String {
  val output = producer
    .produce(
      context
        .with("requestUrl", requestParams.requestUrl)
        .with("identifier", requestParams.queryParams["identifier"]!!)
        .with("password", requestParams.queryParams["password"]!!),
      createSessionUrl,
    )
    .getOrThrow()
  context = output.context
  return output.value.toString()
}
