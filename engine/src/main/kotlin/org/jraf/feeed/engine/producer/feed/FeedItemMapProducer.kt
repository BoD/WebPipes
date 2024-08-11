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

package org.jraf.feeed.engine.producer.feed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import org.jraf.feeed.api.feed.Feed
import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.api.producer.ProducerOutput
import org.jraf.feeed.api.producer.value
import org.jraf.feeed.engine.producer.core.pipe

class FeedItemMapProducer(
  private val mapper: Producer<FeedItem, FeedItem>,
) : Producer<Feed, Feed> {

  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override suspend fun produce(context: ProducerContext, input: Feed): Result<Pair<ProducerContext, Feed>> {
    return runCatching {
      val asyncResults: List<Deferred<Result<ProducerOutput<FeedItem>>>> =
        input.items.map { feedItem -> coroutineScope.async { mapper.produce(context, feedItem) } }
      context to input.copy(
        items = List(input.items.size) { i ->
          // Note: the mapped item's context is lost
          asyncResults[i].await().getOrThrow().value
        }
      )
    }
  }

  override fun close() {
    mapper.close()
    coroutineScope.cancel()
  }
}

fun <IN> Producer<IN, Feed>.feedItemMap(mapper: Producer<FeedItem, FeedItem>): Producer<IN, Feed> {
  return pipe(FeedItemMapProducer(mapper))
}
