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

import org.jraf.feeed.api.feed.Feed
import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.api.producer.ProducerOutput
import org.jraf.feeed.engine.producer.core.addToContextIfNotNull
import org.jraf.feeed.engine.producer.core.pipe

class FeedFilterProducer : Producer<Feed, Feed> {
  override suspend fun produce(context: ProducerContext, input: Feed): Result<ProducerOutput<Feed>> {
    val field: FeedItem.Field<Boolean> = context["field"]
    return runCatching {
      context to input.copy(items = input.items.filter { it[field] })
    }
  }

  override fun close() {}
}

fun <IN> Producer<IN, Feed>.feedFilter(
  field: FeedItem.Field<Boolean>? = null,
): Producer<IN, Feed> {
  return addToContextIfNotNull("field", field)
    .pipe(FeedFilterProducer())
}
