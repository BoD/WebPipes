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

package org.jraf.feeed.engine.producer.generic

import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.api.producer.ProducerOutput
import org.jraf.feeed.api.producer.value

class FeedItemMapFieldProducer<T>(
  private val mapper: Producer<T, T>,
) : Producer<FeedItem, FeedItem> {
  override suspend fun produce(context: ProducerContext, input: FeedItem): Result<ProducerOutput<FeedItem>> {
    val fieldIn: FeedItem.Field<T> = context["fieldIn"]
    val fieldOut: FeedItem.Field<T> = context["fieldOut"]

    return runCatching {
      val fieldCurrentValue: T = input[fieldIn]
      val fieldNewValue = mapper.produce(context, fieldCurrentValue).getOrThrow()
      // Note: the mapped field's context is lost
      context to input.with(fieldOut, fieldNewValue.value)
    }
  }

  override fun close() {
    mapper.close()
  }
}
